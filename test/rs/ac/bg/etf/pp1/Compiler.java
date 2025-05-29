package rs.ac.bg.etf.pp1;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

import java_cup.runtime.Symbol;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import rs.ac.bg.etf.pp1.ast.Program;
import rs.ac.bg.etf.pp1.util.Log4JUtils;
import rs.etf.pp1.mj.runtime.Code;

public class Compiler {
	
	

	static {
		DOMConfigurator.configure(Log4JUtils.instance().findLoggerConfigFile());
		Log4JUtils.instance().prepareLogFile(Logger.getRootLogger());
	}
	
	public static void main(String[] args) throws Exception {
		
		Logger log = Logger.getLogger(Compiler.class);
		
		 if (args.length == 0) {
	        log.error("Nema prosleđene putanje do .mj fajla.");
	        System.exit(1); // Završava program ako nije prosleđena putanja
	    }
		
		Reader br = null;
		try {
			File sourceCode = new File(args[0]);
			log.info("Compiling source file: " + sourceCode.getAbsolutePath());
			
			br = new BufferedReader(new FileReader(sourceCode));
			Yylex lexer = new Yylex(br);
			
			MJParser p = new MJParser(lexer);
	        Symbol s = p.parse();  //pocetak parsiranja
	        
	        
	        Program prog = (Program)(s.value); 
			// ispis sintaksnog stabla
			log.info(prog.toString(""));
			log.info("===================================");
			if(p.errorDetected == true){
	        	log.error("Parsiranje NIJE uspesno zavrseno - Greska pri sintaksnoj analizi!");
	        } 
			else{
				 MyTab.init();
	
				// ispis prepoznatih programskih konstrukcija
				SemanticAnalyzer v = new SemanticAnalyzer();
				
				prog.traverseBottomUp(v);
				
				log.info("===================================");
				MyTab.dump();
				if(!v.passed()){
					log.error("Parsiranje NIJE uspesno zavrseno - Greska pri semantickoj analizi!");
				} else{
					// Generisanje koda
					if (args.length == 1) {
				        log.error("Nema prosleđene putanje do .obj fajla.");
				        System.exit(1); // Završava program ako nije prosleđena putanja
				    }
					File objFile = new File(args[1]);
					if(objFile.exists()){
						objFile.delete();
					}
					CodeGenerator codeGenerator = new CodeGenerator();
					prog.traverseBottomUp(codeGenerator);
					Code.dataSize = v.nVars;
					Code.mainPc = codeGenerator.getMainPc();
					Code.write(new FileOutputStream(objFile));
				}
			}
				
	        
			
		} 
		finally {
			if (br != null) try { br.close(); } catch (IOException e1) { log.error(e1.getMessage(), e1); }
		}

	}
			
}
