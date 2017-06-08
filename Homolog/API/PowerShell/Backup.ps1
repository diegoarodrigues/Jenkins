
###  --------------------------------        BACKUP          --------------------------------####

cd "E:\\ScriptsJenkins"

$FOLDER = Get-Date -Format ddMMyyyyhh
$FOLDER_BKP = "E:\BackupHomolog"
$DEST_BKP = "$FOLDER_BKP\$FOLDER\"
$ftp = "/API_Producao/*"
$BKP_PROD_ALL = $DEST_BKP + "bkp_prod_all"
$bkp_prod_arq_build = $DEST_BKP + "bkp_prod_arq_build"
$bkp_publish_api = $DEST_BKP + "bkp_publish_api"
$bkp_merge = $DEST_BKP + "bkp_merge"
$bkp_rollback = $DEST_BKP + "bkp_rollback"
$PUBLISH = "E:\PublishHomolog"

New-Item $DEST_BKP -type directory
New-Item $BKP_PROD_ALL -type directory
New-Item $bkp_prod_arq_build -type directory
New-Item $bkp_publish_api -type directory
New-Item $bkp_merge -type directory
New-Item $bkp_rollback -type directory



# Load WinSCP .NET assembly

Add-Type -Path "WinSCPnet.dll"

# Setup session options
$sessionOptions = New-Object WinSCP.SessionOptions -Property @{
    Protocol = [WinSCP.Protocol]::Ftp
    HostName = "desenv.ordomederi.com" 
    UserName = "desenv.ordomederi.com|medbarra\arthur.santos"
    Password = "medarthur123"
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


Remove-Item "E:\MergeTeste\*" -Recurse
Copy-Item "$FOLDER_BKP\$FOLDER\bkp_merge\*" -Recurse -Force "E:\MergeTeste\"

###  --------------------        GERA O BKP_ROLLBACK          --------------------------------####


$folder = (Get-ChildItem E:\BackupHomolog | ? { $_.PSIsContainer } | sort CreationTime)[-1]
$path_dir = "E:\BackupHomolog\" + $folder
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