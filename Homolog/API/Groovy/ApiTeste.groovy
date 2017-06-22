@Grapes([
    @Grab(group='commons-io', module='commons-io', version='2.5'),
    @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7'),
    @Grab('net.sf.json-lib:json-lib:2.3:jdk15')
    
])


node {
    stage("VerificaSeTemBuild"){
      echo VerificaDir("C:\\PublishApi");
    }
	stage("Backup") {
        echo "Backup"
		bat 'powershell "C:\\Users\\PublisherTS\\Desktop\\Shells\\teste.ps1" -Verbose'
		//bat 'powershell "C:\\Works\\PowerShell_testeJenkins\\Teste1.ps1" -Verbose'
		//PowerShell(". 'C:\\Works\\PowerShell_testeJenkins\\Teste1.ps1'")
    }
    stage("build") {
        echo "build"
    }
    stage("teste"){
        echo "teste"
    }
    //bat "powershell -noprofile -command \"$ErrorActionPreference = 'Stop'; Set-PsDebug -Strict; C:\\Works\\PowerShell_testeJenkins\\Teste1.ps1\""
    //bat "powershell C:\\Works\\PowerShell_testeJenkins\\Teste1.ps1"
}

@NonCPS
def VerificaDir(folder) { 
    
    def count = 0;
    
    new File(folder).eachFile() { file->
     //println file.getAbsolutePath();
     count = count + 1;
    } 
    
    if (count>0){
        //echo "ok";
        return "ok";
    }
    else {
        //echo "1";
        return "error";
    }
}