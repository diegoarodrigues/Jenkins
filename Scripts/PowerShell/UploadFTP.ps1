###  --------------------------------    CRIA DIRETORIO          --------------------------------####
Param(
  [string] $_ArqDeConfig
)

# Seta os valores do arquivo de configuração para as constantes
$Config = New-Object System.Collections.ArrayList
$data = get-content $_ArqDeConfig
$data | foreach {
   $items = $_.split("=")
		$Config.Add($items[1])
}

$FOLDER_BKP 	= $Config[0];
$ftp 			= $Config[12];
$user 			= $Config[4];
$pass 			= $Config[5];
$Publish 		= $Config[6];
$MergeTeste		= $Config[7];

cd $Publish

function criaDiretorio ($path) {
    try
    {
        $makeDirectory = [System.Net.WebRequest]::Create($path);
        $makeDirectory.Credentials = New-Object System.Net.NetworkCredential($user,$pass);
        $makeDirectory.Method = [System.Net.WebRequestMethods+FTP]::MakeDirectory;
        $makeDirectory.GetResponse();
    
        #folder created successfully
    
    }catch [Net.WebException] 
    {
        try {
    
            #if there was an error returned, check if folder already existed on server
            $checkDirectory = [System.Net.WebRequest]::Create($path);
            $checkDirectory.Credentials = New-Object System.Net.NetworkCredential($user,$pass);
            $checkDirectory.Method = [System.Net.WebRequestMethods+FTP]::PrintWorkingDirectory;
            $response = $checkDirectory.GetResponse();

            #folder already exists!
        }
        catch [Net.WebException] {				
            #if the folder didn't exist, then it's probably a file perms issue, incorrect credentials, dodgy server name etc
            return false
        }	
    }
}

foreach($item in get-childitem -Recurse -Directory) {
   
    $path = $item.FullName.Replace($Publish,$ftp).Replace("\","/")
    criaDiretorio($path)
}

###  --------------------------------        UPLOAD          --------------------------------####

$webclient = New-Object System.Net.WebClient
$webclient.credentials =  New-Object System.Net.NetworkCredential($user,$pass)  

# sobe os arquivos do diretorio sobrescrevendo se tiver nomes repetidos
foreach($item in get-childitem -Recurse -File) 
{
    $path = $item.FullName.Replace($item.FullName,$ftp).Replace("\","/")
    $way = $item.FullName.Replace($Publish,"").Replace("\","/")
    $uri = New-Object System.Uri($path+$way)
    $webclient.UploadFile($uri,$item.FullName)
}

####---------- APAGA ITENS ----####

$folderHomolog = "$Publish\*"
Remove-Item $folderHomolog -Recurse
Remove-Item "$MergeTeste\*" -Recurse

exit $LASTEXITCODE