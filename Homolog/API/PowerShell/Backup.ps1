###  --------------------------------        BACKUP          --------------------------------####
Param(
  [string] $_ArqDeConfig
)

# Seta os valores do arquivo de configuração para as constantes
$Config = New-Object System.Collections.ArrayList
$data = get-content $_ArqDeConfig
$data | foreach {
   $items = $_.split("=")
   if ($items[0] -eq "FOLDER_BKP"){
		$Config.Add($items[1])
   }
}

$FOLDER = Get-Date -Format ddMMyyyyhh
$FOLDER_BKP 	= $Config[0];
$DEST_BKP 		= $Config[1];
$FTP 			= $Config[2];
$WINSCPNET 		= $Config[3];
$HOSTNAME 		= $Config[4];
$USERNAME 		= $Config[5];
$PASSWORD 		= $Config[6];
$PUBLISH 		= $Config[7];
$MERGETESTE 	= $Config[8];
$SCRIPTSJENKINS = $Config[9];

$BKP_PROD_ALL = $DEST_BKP + "bkp_prod_all"
$BKP_PROD_ARQ_BUILD = $DEST_BKP + "bkp_prod_arq_build"
$BKP_PUBLISH_API = $DEST_BKP + "bkp_publish_api"
$BKP_MERGE = $DEST_BKP + "bkp_merge"
$BKP_ROLLBACK = $DEST_BKP + "bkp_rollback"

cd SCRIPTSJENKINS

New-Item $DEST_BKP -type directory
New-Item $BKP_PROD_ALL -type directory
New-Item $BKP_PROD_ARQ_BUILD -type directory
New-Item $BKP_PUBLISH_API -type directory
New-Item $BKP_MERGE -type directory
New-Item $BKP_ROLLBACK -type directory

# Load WinSCP .NET assembly
Add-Type -Path $WINSCPNET

# Setup session options
$sessionOptions = New-Object WinSCP.SessionOptions -Property @{
    Protocol = [WinSCP.Protocol]::Ftp
    HostName = $HOSTNAME
    UserName = $USERNAME
    Password = $PASSWORD
}

$session = New-Object WinSCP.Session
try
{
    # Connect
    $session.Open($sessionOptions)

    # Download files
    $session.GetFiles($ftp, "$BKP_PROD_ALL\").Check()
}
finally
{
    # Disconnect, clean up
    $session.Dispose()
} 

Copy-Item $PUBLISH\* -Recurse "$FOLDER_BKP\$FOLDER\bkp_publish_api\"

$caminho = "$FOLDER_BKP\$FOLDER\bkp_prod_all\bin"
$destino = "$FOLDER_BKP\$FOLDER\bkp_prod_arq_build\"

New-Item "$FOLDER_BKP\$FOLDER\bkp_prod_arq_build\bin" -type directory

Copy-Item $caminho\AWSSDK.dll -Recurse "$destino\bin"
Copy-Item $caminho\Business.dll -Recurse "$destino\bin"
Copy-Item $caminho\Medgrupo.DataAccessEntity.dll -Recurse "$destino\bin"
Copy-Item $caminho\Medgrupo.RestfulService.dll -Recurse "$destino\bin"
Copy-Item $caminho\UIDataHelper.dll -Recurse "$destino\bin"
Copy-Item $caminho\EntityFramework.dll -Recurse "$destino\bin"

Copy-Item "$FOLDER_BKP\$FOLDER\bkp_prod_arq_build\*" -Recurse -Force "$FOLDER_BKP\$FOLDER\bkp_merge\"

Copy-Item "$FOLDER_BKP\$FOLDER\bkp_publish_api\*" -Recurse -Force "$FOLDER_BKP\$FOLDER\bkp_merge\"

#Remove-Item "$MERGETESTE\*" -Recurse
Copy-Item "$FOLDER_BKP\$FOLDER\bkp_merge\*" -Recurse -Force "$MERGETESTE\"

###  --------------------        GERA O BKP_ROLLBACK          --------------------------------####

$folder = (Get-ChildItem $FOLDER_BKP | ? { $_.PSIsContainer } | sort CreationTime)[-1]
$path_dir = $FOLDER_BKP + "\" + $folder
cd "$path_dir\bkp_publish_api"

(get-childitem -Recurse).FullName | out-file arquivos.txt

foreach($itemDir in get-childitem -Recurse -Directory) {
    $path = $itemDir.FullName.Replace("bkp_publish_api", "bkp_rollback")
    New-Item -Path $path -ItemType "directory"
}

$dir_Prod = $path_dir + "\bkp_prod_all"
$dir_Rollback = $path_dir + "\bkp_rollback"

$ListaArquivos = Get-Content arquivos.txt
$Lista_Prod_All = Get-ChildItem -Recurse $dir_Prod -File

foreach ($Item in $ListaArquivos)
    {
    foreach ($arquivo in $Lista_Prod_All)
        {
         $name = $Item.Replace("bkp_publish_api","bkp_prod_all")
        if ($arquivo.FullName -like $name )
           {
            Copy-Item $arquivo.FullName -Destination $arquivo.FullName.Replace("bkp_prod_all", "bkp_rollback").Replace("\$arquivo.Name", "")
           }
        }
    }

rm arquivos.txt

exit $LASTEXITCODE