###  --------------------------------   CRIA DIRETORIO   2       --------------------------------####

$ftp = "ftp://desenv.ordomederi.com/API_Producao" 
$user ="desenv.ordomederi.com|medbarra\arthur.santos"
$pass = "medarthur123"

$folder = (Get-ChildItem E:\BackupHomolog| ? { $_.PSIsContainer } | sort CreationTime)[-1]
$way = "E:\BackupHomolog"
$source= "E:\BackupHomolog\$folder\bkp_rollback"

cd $way

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
   
    $path = $item.FullName.Replace($source,$ftp).Replace("\","/")
    if($path.StartsWith("ftp"))
    {
      
      criaDiretorio($path)
    }
}

###  --------------------        Rollback segunda parte          --------------------------------####

cd E:\BackupHomolog
#pega a pasta de backup que sera feito o rollback
$folder = (Get-ChildItem E:\BackupHomolog | ? { $_.PSIsContainer } | sort CreationTime)[-1]
$var = "$folder\bkp_rollback"
cd $var



$webclient = New-Object System.Net.WebClient
$webclient.credentials =  New-Object System.Net.NetworkCredential($user,$pass)  

# sobe os arquivos do diretorio sobrescrevendo se tiver nomes repetidos
foreach($item in get-childitem -Recurse -File) 
{
    $path = $item.FullName.Replace("E:\BackupHomolog\"+$var,$ftp).Replace("\","/")
    $way = $item.FullName.Replace($folder.FullName,"").Replace("\","/")
    $uri = New-Object System.Uri($path).Replace("/bkp_rollback","")
    

    
    $webclient.UploadFile($uri,$item.FullName)
}