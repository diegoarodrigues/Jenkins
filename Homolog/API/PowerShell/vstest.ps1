    ###  --------------------        Rename          --------------------------------####
$path = "E:\BackupHomolog"
$folder = (Get-ChildItem $path| ? { $_.PSIsContainer } | sort CreationTime)[-1]
$source = "$path\$folder"
$minSeg = Get-Date -Format mmss
$newName = "$source"+ "_" + "$minSeg"

Rename-Item -path $source $newName



$exePath = "C:\Program Files (x86)\Microsoft Visual Studio 11.0\Common7\IDE\CommonExtensions\Microsoft\TestWindow\vstest.console.exe"

$argList = "E:\MergeTeste\bin\TrocaDevice.orderedtest"
$argList2 = "E:\MergeTeste\bin\Medgrupo.RestfulService.Tests.dll"

& $exePath $argList
& $exePath $argList2

exit $LASTEXITCODE
