@Grapes([
    @Grab(group='commons-io', module='commons-io', version='2.5'),
    @Grab('org.codehaus.groovy.modules.http-builder:http-builder:0.7'),
    @Grab('net.sf.json-lib:json-lib:2.3:jdk15')
    
])

import groovy.json.JsonSlurper;
import org.apache.commons.io.IOUtils;
import groovyx.net.http.*;
import groovyx.net.http.ContentType.*;
import groovyx.net.http.Method.*;
import net.sf.json.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedInputStream;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.BufferedInputStream.*;

String DIR_PUBLISH_API 	= "E:\\PublishHomolog";
String SCRIPTS 			= "E:\\ScriptsJenkins\\";
String SCRIPTS_HOMOLOG 	= "E:\\ScriptsJenkins\\Homolog\\API\\PowerShell";
String RunScopeOk 		= "";
String RUNSCOPE_TRIGGER = "https://api.runscope.com/radar/9d491522-e64b-4632-bc68-2e943a959d28/trigger";
String RUNSCOPE_TESTE 	= "https://api.runscope.com/buckets/wynpst2ckqyc/tests/062a1f6c-6a72-49dc-86dd-312c9e4cfd3e/results/";
String RUNSCOPE_TOKEN 	= 'Bearer fce0e02f-71d4-47d1-88b4-911d5ebc46ae';
String ARQ_CONFIG 		= "E:\\ScriptsJenkins\\Homolog\\API\\PowerShell\\Config.txt";
String RunScopeDepoisDoLoadBalanceOk = "";

node {
	
	//stage("teste"){
		// mail (to: 'diego.rodrigues@medgrupo.com.br',
         // subject: "Job '${env.JOB_NAME}' (${env.BUILD_NUMBER}) is waiting for input",
         // body: "Please go to ${env.BUILD_URL}.");
	//	 emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Backup Homolog', to: 'diego.rodrigues@medgrupo.com.br';
	//}
	
    stage("VerificaSeTemBuild"){
      if(VerificaDir(DIR_PUBLISH_API) == "ok"){
        echo "ok";
      }
      else{
          echo "Não existe arquivos no publish para build. Caminho: " + DIR_PUBLISH_API;
          exit;
      }
    }
    
	stage("Backup") {
	    string backup = SCRIPTS_HOMOLOG + "\\Backup.ps1";
        def powerS = bat (script: 'powershell "'+backup+'" "'+ ARQ_CONFIG +'"', returnStatus: true)
		if(powerS == 0){
			echo "ok";
		}
		else{
		    echo "Erro no backup";
		    emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Backup Homolog', to: 'diego.rodrigues@medgrupo.com.br';
		    exit;
		}
    }
    
    stage("build") {
        echo "build"
    }
    
    stage("QA"){
        string VsTest = SCRIPTS_HOMOLOG + "\\vstest.ps1";        
        def powerS = bat (script: 'powershell "'+VsTest+'" "'+ ARQ_CONFIG +'"', returnStatus: true)
		if(powerS == 0){
		    echo "ok";
		}else{
		    echo "Erro no teste";
			emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Teste Homologação', to: 'diego.rodrigues@medgrupo.com.br';
		    exit;
		}
    }
    
    stage("UploadFTP"){
        string UploadFtp = SCRIPTS_HOMOLOG + "\\UploadFTP.ps1";
        def powerS = bat (script: 'powershell "'+UploadFtp+'" "'+ ARQ_CONFIG +'"', returnStatus: true)
        
		if(powerS == 0){
		    echo "ok";
		}else{
		    echo "Erro no Upload";
			emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Upload Homologação', to: 'diego.rodrigues@medgrupo.com.br';
		    exit;
		}
    }
    
    stage("RunScope"){
        String url = RUNSCOPE_TRIGGER;
        def objRest = ChamaRest(url, RUNSCOPE_TOKEN);
        String idTrigger = objRest.data.runs.test_run_id[0].toString();
        RunScopeOk = ChamaRestTeste(idTrigger, RUNSCOPE_TESTE, RUNSCOPE_TOKEN);
        while(RunScopeOk != "pass" && RunScopeOk != "fail")
        {
          RunScopeOk = ChamaRestTeste(idTrigger, RUNSCOPE_TESTE, RUNSCOPE_TOKEN);
        }
		
		if(RunScopeOk == "pass"){
			echo "ok";
		}
		else
		{
			echo "Erro no RunScope";
			exit;
		}
    }
	
	stage("E-mail RunScope"){
		if(RunScopeOk == "pass"){
			emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'RunScope Homologação', to: 'diego.rodrigues@medgrupo.com.br';
		}
	}
    
    stage("Rollback"){
        if (RunScopeOk != "pass"){
			echo "Rollback";
			string rollback = SCRIPTS_HOMOLOG + "\\Rollback.ps1";
			def powerSRollback = bat (script: 'powershell "'+ rollback +'"', returnStatus: true)
			if(powerSRollback == 0){
				echo "ok";
				emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Rollback Homologação', to: 'diego.rodrigues@medgrupo.com.br';
			}else{
				echo "Erro no Upload";
				emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Rollback Homologação', to: 'diego.rodrigues@medgrupo.com.br';
				exit;
			}
		}
		else{
			echo "Sem Rollback";
		}
    }
	
	stage("RunScopeDepoisDoLoadBalance"){
		sleep time: 6, unit: 'MINUTES';
        String url = RUNSCOPE_TRIGGER;
        def objRest = ChamaRest(url, RUNSCOPE_TOKEN);
        String idTrigger = objRest.data.runs.test_run_id[0].toString();
        RunScopeDepoisDoLoadBalanceOk = ChamaRestTeste(idTrigger, RUNSCOPE_TESTE, RUNSCOPE_TOKEN);
        while(RunScopeDepoisDoLoadBalanceOk != "pass" && RunScopeDepoisDoLoadBalanceOk != "fail")
        {
          RunScopeDepoisDoLoadBalanceOk = ChamaRestTeste(idTrigger, RUNSCOPE_TESTE, RUNSCOPE_TOKEN);
        }
		
		if(RunScopeDepoisDoLoadBalanceOk == "pass"){
			echo "ok";
		}
		else
		{
			echo "Erro no RunScope";
			//exit;
		}
    }
	
	stage("E-mail RunScope depois do LoadBalance Homologação"){
		if(RunScopeDepoisDoLoadBalanceOk == "pass"){
			emailext attachLog: true, body: 'RunScope executado com sucesso.', subject: 'RunScope depois do LoadBalance Homologação', to: 'diego.rodrigues@medgrupo.com.br';
		}
		else{
			emailext attachLog: true, body: 'RunScope com erro ao executar.', subject: 'RunScope depois do LoadBalance Homologação', to: 'diego.rodrigues@medgrupo.com.br';
		}
	}
	
	stage("RollbackDepoisDoLoadBalance"){
        if (RunScopeDepoisDoLoadBalanceOk != "pass"){
			echo "Rollback";
			string rollback = SCRIPTS_HOMOLOG + "\\Rollback.ps1";
			def powerSRollback = bat (script: 'powershell "'+ rollback +'"', returnStatus: true)
			if(powerSRollback == 0){
				echo "ok";
				emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Rollback depois do loadBalance Homologação', to: 'diego.rodrigues@medgrupo.com.br';
			}else{
				echo "Erro no Upload";
				emailext attachLog: true, body: '$PROJECT_DEFAULT_CONTENT', subject: 'Rollback depois do loadBalance Homologação', to: 'diego.rodrigues@medgrupo.com.br';
			}
		}
		else{
			echo "Sem Rollback";
		}
    }
    
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
   
 
public def ChamaRest(def url, String token){
    URL object = new  URL(url);

    HttpURLConnection connection = (HttpURLConnection) object
            .openConnection();
    // int timeOut = connection.getReadTimeout();
    connection.setReadTimeout(60 * 1000);
    connection.setConnectTimeout(60 * 1000);

    connection.setRequestProperty("Authorization", token);
    int responseCode = connection.getResponseCode();
    //String responseMsg = connection.getResponseMessage();

    if (responseCode == 200 || responseCode == 201) {
        InputStream inputStr = connection.getInputStream();
        
        String encoding = connection.getContentEncoding() == null ? "UTF-8"
                : connection.getContentEncoding();
        jsonResponse = IOUtils.toString(inputStr, encoding);
        
      def jsonSlurper = new JsonSlurper()
      def obj = jsonSlurper.parseText(jsonResponse) 
      //println(obj.data.result);
      return obj;
    }
}

public String ChamaRestTeste(def teste, String urlTeste, String token){
    URL object = new  URL(urlTeste+ teste);

    HttpURLConnection connection = (HttpURLConnection) object
            .openConnection();
    // int timeOut = connection.getReadTimeout();
    connection.setReadTimeout(60 * 1000);
    connection.setConnectTimeout(60 * 1000);

    connection.setRequestProperty("Authorization", token);
    int responseCode = connection.getResponseCode();
    //String responseMsg = connection.getResponseMessage();

    if (responseCode == 200) {
        InputStream inputStr = connection.getInputStream();
        
        String encoding = connection.getContentEncoding() == null ? "UTF-8"
                : connection.getContentEncoding();
        jsonResponse = IOUtils.toString(inputStr, encoding);
        
      def jsonSlurper = new JsonSlurper()
      def obj = jsonSlurper.parseText(jsonResponse) 
      //println(obj.data.result);
      return obj.data.result;
    }
}