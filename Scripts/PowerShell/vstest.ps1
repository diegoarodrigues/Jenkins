###  --------------------        Rename          --------------------------------####
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

$FOLDER_BKP = $Config[0];
$EXEPATH = $Config[9];
$ARGLIST = $Config[10];
$ARGLIST2 = $Config[11];

$path = $FOLDER_BKP;
$folder = (Get-ChildItem $path| ? { $_.PSIsContainer } | sort CreationTime)[-1]
$source = "$path\$folder"
$minSeg = Get-Date -Format mmss
$newName = "$source"+ "_" + "$minSeg"

Rename-Item -path $source $newName
$exePath = $EXEPATH
#$argList = $ARGLIST
#$argList2 = $ARGLIST2

#& $exePath $argList
#& $exePath $argList2
$argListArray = $ARGLIST.split(';')
foreach ($arg in $argListArray){
	& $exePath $arg
}

exit $LASTEXITCODE




