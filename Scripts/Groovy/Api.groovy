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

import java.io.File;
import java.io.FileInputStream;
import java.io.*;


def config = configuracao("E:\\ScriptsJenkins\\Homolog\\Inscricoes\\Config.txt");
 
String DIR_PUBLISH     		= config["PUBLISH"];
String SCRIPTS             	= config["SCRIPTSJENKINS"];
String SCRIPTS_HOMOLOG     	= config["SCRIPTS_HOMOLOG"];
String RUNSCOPE_TRIGGER 	= config["RUNSCOPE_TRIGGER"];
String RUNSCOPE_TESTE     	= config["RUNSCOPE_TESTE"];
String RUNSCOPE_TOKEN     	= config["RUNSCOPE_TOKEN"];
String ARQ_CONFIG         	= config["ARQ_CONFIG"];
String EMAIL_TO				= config["EMAIL_TO"];

String MAIL_SUBJECT_RUNSCOPE					= config["MAIL_SUBJECT_RUNSCOPE"];
String MAIL_SUBJECT_BACKUP						= config["MAIL_SUBJECT_BACKUP"];
String MAIL_SUBJECT_TESTE						= config["MAIL_SUBJECT_TESTE"];
String MAIL_SUBJECT_UPLOAD						= config["MAIL_SUBJECT_UPLOAD"];
String MAIL_SUBJECT_ROLLBACK					= config["MAIL_SUBJECT_ROLLBACK"];
String MAIL_SUBJECT_ROLLBACK_DEPOIS_LBALANCE	= config["MAIL_SUBJECT_ROLLBACK_DEPOIS_LBALANCE"];
String MAIL_SUBJECT_RUNSCOPE_DEPOIS_LBALANCE	= config["MAIL_SUBJECT_RUNSCOPE_DEPOIS_LBALANCE"];

String RunScopeOk 							= "";
String RunScopeDepoisDoLoadBalanceOk 		= "";

node {
    
    stage("VerificaSeTemBuild"){
      if(VerificaDir(DIR_PUBLISH) == "ok"){
        echo "Build ok";
      }
      else{
          echo "Não existe arquivos no publish para build. Caminho: " + DIR_PUBLISH;
          exit;
      }
    }
    
	stage("Backup") {
	    string backup = SCRIPTS_HOMOLOG + "\\Backup.ps1";
        def powerS = bat (script: 'powershell "'+backup+'" "'+ ARQ_CONFIG +'"', returnStatus: true)
		if(powerS == 0){
			echo "Backup feito com sucesso";
		}
		else{
		    echo "Erro no backup";
		    emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_BACKUP, to: EMAIL_TO;
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
			emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_TESTE, to: EMAIL_TO;
		    exit;
		}
    }
    
    stage("UploadFTP"){
		exit;
        string UploadFtp = SCRIPTS_HOMOLOG + "\\UploadFTP.ps1";
        def powerS = bat (script: 'powershell "'+UploadFtp+'" "'+ ARQ_CONFIG +'"', returnStatus: true)
        
		if(powerS == 0){
		    echo "ok";
		}else{
		    echo "Erro no Upload";
			emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_UPLOAD, to: EMAIL_TO;
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
			emailext attachLog: true, body: 'Feito com sucesso.', subject: MAIL_SUBJECT_RUNSCOPE, to: EMAIL_TO;
		}
	}
    
    stage("Rollback"){
        if (RunScopeOk != "pass"){
			echo "Rollback";
			string rollback = SCRIPTS_HOMOLOG + "\\Rollback.ps1";
			def powerSRollback = bat (script: 'powershell "'+ rollback +'"', returnStatus: true)
			if(powerSRollback == 0){
				echo "ok";
				emailext attachLog: true, body: 'Feito com sucesso.', subject: MAIL_SUBJECT_ROLLBACK, to: EMAIL_TO;
			}else{
				echo "Erro no Upload";
				emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_ROLLBACK, to: EMAIL_TO;
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
			echo "RunScope ok";
		}
		else
		{
			echo "Erro no RunScope";
			//exit;
		}
    }
	
	stage("E-mail RunScope depois do LoadBalance Homologação"){
		if(RunScopeDepoisDoLoadBalanceOk == "pass"){
			emailext attachLog: true, body: 'Feito com sucesso.', subject: MAIL_SUBJECT_RUNSCOPE_DEPOIS_LBALANCE, to: EMAIL_TO;
		}
		else{
			emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_RUNSCOPE_DEPOIS_LBALANCE, to: EMAIL_TO;
		}
	}
	
	stage("RollbackDepoisDoLoadBalance"){
        if (RunScopeDepoisDoLoadBalanceOk != "pass"){
			echo "Rollback";
			string rollback = SCRIPTS_HOMOLOG + "\\Rollback.ps1";
			def powerSRollback = bat (script: 'powershell "'+ rollback +'"', returnStatus: true)
			if(powerSRollback == 0){
				echo "ok";
				emailext attachLog: true, body: 'Feito com sucesso.', subject: MAIL_SUBJECT_ROLLBACK_DEPOIS_LBALANCE, to: EMAIL_TO;
			}else{
				echo "Erro no Upload";
				emailext attachLog: true, body: 'Falhou.', subject: MAIL_SUBJECT_ROLLBACK_DEPOIS_LBALANCE, to: EMAIL_TO;
			}
		}
		else{
			echo "Sem Rollback";
		}
    }
    
}


@NonCPS
def configuracao(String arqConfig) {
    Map<String, Integer> config = new HashMap<String, Integer>();
    FileInputStream fstream = new FileInputStream(arqConfig);
    
    BufferedReader br = new BufferedReader(new InputStreamReader(fstream));
    String  strLine;
   
    while ((line = br.readLine()) != null){
        lineSplit = line.split("=");
        config.put(lineSplit[0], lineSplit[1])
    }

    br.close();
    return config;
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
