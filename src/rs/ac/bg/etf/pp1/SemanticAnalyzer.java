package rs.ac.bg.etf.pp1;

import java.nio.file.WatchEvent.Kind;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.*;

public class SemanticAnalyzer extends VisitorAdaptor {


	int printCallCount = 0;
	int varDeclCount = 0;
	boolean returnFounded = false;
	boolean errorDetected = false;
	int nVars;
	
	Struct currLiteral=null;
	Struct currType=null;
	int currConstLiteral;
	int currParameters = 0;
	Obj currMethod=null;
	boolean mainFounded = false;
	boolean isLastVarArray = false;
	boolean insideDoWhile = false;
	String currRelOp = "";
	Map<String, List<Obj>> functionParameters = new HashMap<String, List<Obj>>();
	List<Struct> currActParams = new ArrayList<Struct>();
	
	Logger log = Logger.getLogger(getClass());
	
	public void report_error(String message, SyntaxNode info) {
		errorDetected = true;
		StringBuilder msg = new StringBuilder(message);
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.error(msg.toString());
	}

	public void report_info(String message, SyntaxNode info) {
		StringBuilder msg = new StringBuilder(message); 
		int line = (info == null) ? 0: info.getLine();
		if (line != 0)
			msg.append (" na liniji ").append(line);
		log.info(msg.toString());
	}
	
	public boolean passed(){
		return !errorDetected;
	}
	
    public void visit(ProgName progName){
    	progName.obj = MyTab.insert(Obj.Prog, progName.getProgramName(), MyTab.noType);
    	MyTab.openScope();
    	//MyTab.currentScope.addToLocals(new Obj(Obj.Meth, "1mapFooName", MyTab.intType, 0, 1));
    	//MyTab.currentScope.addToLocals(new Obj(Obj.Var, "1mapFooArr", new Struct(Struct.Array, MyTab.intType), 0, 0));
    	MyTab.currentScope.addToLocals(new Obj(Obj.Var, "1mapFooSum", MyTab.intType, 0, 0));
    	MyTab.currentScope.addToLocals(new Obj(Obj.Var, "1mapFooIndex", MyTab.intType, 1, 0));
    	initPredefinedFunctions();
    }
    
    public void initPredefinedFunctions(){
    	List<Obj> params = new ArrayList<Obj>();
    	// chr Function
    	params.add(new Obj(Obj.Var, "i", MyTab.intType));
    	functionParameters.put("chr", params);
    	
    	params = new ArrayList<Obj>();
    	// ord Function
    	params.add(new Obj(Obj.Var, "ch", MyTab.charType));
    	functionParameters.put("ord", params);
    	
    	params = new ArrayList<Obj>();
    	// len Function
    	params.add(new Obj(Obj.Var, "arr", new Struct(Struct.Array, MyTab.intType)));
    	functionParameters.put("len", params);
    	
    	params = new ArrayList<Obj>();
    	// add Function
    	params.add(new Obj(Obj.Var, "a", MyTab.setType));
    	params.add(new Obj(Obj.Var, "b", MyTab.intType));
    	functionParameters.put("add", params);
    	
    	params = new ArrayList<Obj>();
    	// addAll Function
    	params.add(new Obj(Obj.Var, "a", MyTab.setType));
    	params.add(new Obj(Obj.Var, "b", new Struct(Struct.Array, MyTab.intType)));
    	functionParameters.put("addAll", params);
    }
    
    public void visit(Program program){
    	nVars = MyTab.currentScope.getnVars();
    	MyTab.chainLocalSymbols(program.getProgName().obj);
    	MyTab.closeScope();
    }
    
    
    public void visit(NumLiteral numLiteral){
    	currLiteral = MyTab.intType;
    	currConstLiteral = numLiteral.getNumLiteral();
    }
    
    public void visit(CharLiteral charLiteral){
    	currLiteral = MyTab.charType;
    	currConstLiteral = (int)charLiteral.getCharLiteral();
    }
    
    public void visit(BoolLiteral boolLiteral){
    	currLiteral = MyTab.boolType;
    	currConstLiteral = (boolLiteral.getBoolLiteral() == true) ?  1 :  0;
    }
    
    public void visit(Type type){
    	Obj typeNode = MyTab.find(type.getTypeName());
    	if(typeNode == MyTab.noObj){
    		report_error("Nije pronadjen tip " + type.getTypeName() + " u tabeli simbola! ", null);
    		type.struct = MyTab.noType;
    	}else{
    		if(Obj.Type == typeNode.getKind()){
    			type.struct = typeNode.getType();
    			currType = typeNode.getType();
    		}else{
    			report_error("GRESKA NA LINIJI " + type.getLine() + " - ime " + type.getTypeName() + " ne predstavlja tip!", null);
    			type.struct = MyTab.noType;
    		}
    	}
    }
    
    
    public void visit(ConstNonLastDecl constNonLast){
    	visitConst(constNonLast.getIdentName(), constNonLast.getLine());
    }
    
    public void visit(ConstLastDecl constLast){
    	visitConst(constLast.getIdentName(), constLast.getLine());
    }
    
    public void visitConst(String constName, int line){
    	if(MyTab.findInCurrentScope(constName) == MyTab.noObj){
    		if(currType.getKind()==MyStruct.Set){
    			report_error("GRESKA NA LINIJI " + line + " - konstante ne mogu biti tipa set!" , null);
    			return;
    		}
    		if(currLiteral.assignableTo(currType)){
        		Obj constObj = MyTab.insert(Obj.Con, constName, currType);
        		constObj.setAdr(currConstLiteral);
        		report_info("Definisana konstanta " + constName + " na liniji " + line, null);

        	} else{
        		report_error("GRESKA NA LINIJI " + line + " - tipovi nisu kompatibilni!" , null);
        	}
    	} else{
    		report_error("GRESKA NA LINIJI " + line + " - simbol sa imenom " + constName + " je vec definisan!" , null);
    	}	
    }
        
    public void visit(VarNonLastDecl var){
    	visitVar(var.getIdentName(), var.getLine()); 
    }
    
    public void visit(VarLastDecl var){
    	visitVar(var.getIdentName(), var.getLine());
    }
    
    public void visitVar(String varName, int line){
    	if(MyTab.findInCurrentScope(varName) == MyTab.noObj){
    		Obj varObj = null;
    		if(isLastVarArray){
    			if(currType.getKind()==MyStruct.Set){
    				report_error("GRESKA NA LINIJI " + line + " - promenljive tipa set ne mogu biti nizovi!" , null);
    				return;
    			}
    			varObj = MyTab.insert(Obj.Var, varName, new Struct(Struct.Array, currType));
        	} else{
        		varObj = MyTab.insert(Obj.Var, varName, currType);
        	}
    		report_info("Definisana promenljiva " + varName + " na liniji " + line, null);
    	} else{
    		report_error("GRESKA NA LINIJI " + line + " - simbol sa imenom " + varName + " je vec definisan u tekucem opsegu!" , null);
    	}
    }
    
    public void visit (DimPropArray array){
    	isLastVarArray = true;
    }
    
    public void visit (DimPropNonArray nonArray){
    	isLastVarArray = false;
    }
      
    public void visit(TypeMethodTypeOrVoid typeRetVal){
    	typeRetVal.struct = typeRetVal.getType().struct;
    }
    
    public void visit(VoidMethodTypeOrVoid voidRetVal){
    	voidRetVal.struct = MyTab.noType;
    }
    
    public void visit(MethodTypeAndName method){
    	returnFounded = false;
    	if(MyTab.find(method.getMethodName()) != MyTab.noObj){
    		report_error("GRESKA NA LINIJI " + method.getLine() + " - simbol sa imenom " + method.getMethodName() + " je vec definisan!", null);
    		return;
    	}
    	if(method.getMethodName().equals("main")){
    		mainFounded = true;
    		if(method.getMethodTypeOrVoid().struct != MyTab.noType){
    			report_error("GRESKA NA LINIJI " + method.getLine() + " - main metoda nije definisana ispravno(mora biti void)", null);
    			return;
    		} else{
    			report_info("Pronadjena funkcija main!",null);
    		}
    	}
    	method.obj = currMethod = MyTab.insert(Obj.Meth, method.getMethodName(), method.getMethodTypeOrVoid().struct);
    	currParameters = 0;
    	currMethod.setLevel(currParameters);
    	MyTab.openScope();	
    	report_info("Definisana funkcija " + method.getMethodName() + " na liniji " + method.getLine(),null);
    }
    
    public void visit(MethodDecl method){
    	if(currMethod != null){
    		if(!currMethod.getType().equals(MyTab.noType) && returnFounded == false){
    			report_info("GRESKA NA LINIJI " + method.getLine() + " - metoda definisana kao non-void mora da ima return iskaz!",null);
    		}
    		MyTab.chainLocalSymbols(currMethod);
        	MyTab.closeScope();
        	currMethod = null;
    	}	
    }   
    
    public void visit(MethodSignature methodSignature){
    	if(currMethod.getName().equals("main") && currMethod.getLevel()>0){
    		report_error("GRESKA NA LINIJI " + methodSignature.getLine() + " - metoda main ne sme imati formalne parametre!", null);
    	}
    }
    
    public void visit(FormParamNonLastDecl formParamNonLast){
    	visitFormParam(formParamNonLast.getIdentName(), formParamNonLast.getLine());
    }
    
    public void visit(FormParamLastDecl formParamLast){
    	visitFormParam(formParamLast.getIdentName(), formParamLast.getLine());
    }
    
    public void visitFormParam(String formParamName, int line){
    	if(MyTab.findInCurrentScope(formParamName)==MyTab.noObj){
	    	Obj formParamObj = null;
			if(isLastVarArray){
				if(currType.getKind()==MyStruct.Set){
					report_error("GRESKA NA LINIJI " + line + " - promenljive tipa set ne mogu biti nizovi!" , null);
					return;
				}
				formParamObj = MyTab.insert(Obj.Var, formParamName, new Struct(Struct.Array, currType));
	    	} else{
	    		formParamObj = MyTab.insert(Obj.Var, formParamName, currType);
	    	}
			report_info("Definisana promenljiva " + formParamName + " na liniji " + line, null);
			currParameters++;
			currMethod.setLevel(currParameters);
			List<Obj> params = null;
			if(!functionParameters.containsKey(currMethod.getName())){
				params = new ArrayList<Obj>();
			} else{
				params = functionParameters.get(currMethod.getName());
			}
			params.add(formParamObj);
			functionParameters.put(currMethod.getName(), params);
    	} else{
    		report_error("GRESKA NA LINIJI " + line + " - simbol sa imenom " + formParamName + " je vec deklarisan u tekucem opsegu!" , null);
    	}
    }
    
    public void visit(ReturnStatement retStmt){
    	returnFounded=true;
    	if(!currMethod.getType().equals(MyTab.noType)){
    		report_error("GRESKA NA LINIJI " + retStmt.getLine() + " - Metoda koja je definisana kao non-void mora da ima povratnu vrednost!", null);
    	}
    }
    
    public void visit(ReturnExpressionStatement retStmt){
    	returnFounded=true;
    	if(retStmt.getExpression().struct.equals(MyTab.noType)){
    		return;
    	}
    	if(currMethod.getType().equals(MyTab.noType) || !retStmt.getExpression().struct.compatibleWith(currMethod.getType())){
    		report_error("GRESKA NA LINIJI " + retStmt.getLine() + " - Tip metode i povratne vrednosti nisu kompatibilni!", null);
    	}
    }
    
    public void visit(DesignatorVar designator){
    	if(!visitDesignatorDeclared(designator.getDesignIdent(), designator.getLine())){
    		designator.obj = MyTab.noObj;
    		return;
    	}
    	Obj obj = MyTab.find(designator.getDesignIdent());
    	designator.obj = MyTab.find(designator.getDesignIdent());
    	if(obj.getKind() == Obj.Con){
    		report_info("Koriscenje konstante " + obj.getName() + " na liniji " + designator.getLine(), null);
    	} else if(obj.getKind() == Obj.Var){
    		report_info("Koriscenje promenljive " + obj.getName() + " na liniji " + designator.getLine(), null);
    	}
    }
    
    public void visit(DesignatorArray designator){
    	if(designator.getEnterDesignatorArray().obj.equals(MyTab.noObj) || 
    			designator.getExpression().struct.equals(MyTab.noType)){
    		designator.obj = MyTab.noObj;
    		return;
    	}
    	if(!designator.getExpression().struct.compatibleWith(MyTab.intType)){
    		report_error("GRESKA NA LINIJI " + designator.getLine() + " - tip unutar [] mora biti int!" , null);
    		designator.obj = MyTab.noObj;
    	} else{
    		Obj arrObj = designator.getEnterDesignatorArray().obj;
    		designator.obj = new Obj(Obj.Elem, arrObj.getName(), arrObj.getType().getElemType());
    		report_info("Koriscenje elementa niza " + arrObj.getName() + " na liniji " + designator.getLine(), null);
    	}
    }
    
    public void visit(EnterDesignatorArray enterDesignArr){
    	if(!visitDesignatorDeclared(enterDesignArr.getDesignIdent(), enterDesignArr.getLine())){
    		enterDesignArr.obj = MyTab.noObj;
    		return;
    	}
    	Obj obj = MyTab.find(enterDesignArr.getDesignIdent());
    	if(obj.getType().getKind()!=Struct.Array){
    		report_error("GRESKA NA LINIJI " + enterDesignArr.getLine()+ " - simbol sa imenom "+enterDesignArr.getDesignIdent()+" nije niz, a koristi indeksiranje! ", null);
    		enterDesignArr.obj = MyTab.noObj;
    		return;
    	}
    	enterDesignArr.obj = obj;
    }
    
    public boolean visitDesignatorDeclared(String designatorIdent, int line){
    	Obj obj = MyTab.find(designatorIdent);
    	if(obj == MyTab.noObj){
			report_error("GRESKA NA LINIJI " + line+ " - simbol sa imenom "+designatorIdent+" nije definisan!", null);
			return false;
    	}
    	return true;
    }
    
    public void visit(SingleActParamList actParam){
    	currActParams.add(actParam.getExpression().struct);
    }
    
    public void visit(MultActParamList actParam){
    	currActParams.add(actParam.getExpression().struct);
    }
    
    public boolean parametersMatched(Obj functionObj){
    	if(!functionParameters.containsKey(functionObj.getName()) && currActParams.size() == 0){
    		return true;
    	}
    	if((!functionParameters.containsKey(functionObj.getName())) || (currActParams.size() != functionParameters.get(functionObj.getName()).size())){
    		return false;
    	}
    	for(int i=0; i < currActParams.size(); i++){
    		if(!(functionParameters.get(functionObj.getName()).get(i).getType().compatibleWith(currActParams.get(i)))){
    			return false;
    		}
    	}
    	return true;
    }
    
    public void visit(DesignatorStmtAssignSemi designatorStatement){
    	visitDesignStmtAssign(designatorStatement.getDesignator().obj, designatorStatement.getExpression().struct, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtAssignParen designatorStatement){
    	visitDesignStmtAssign(designatorStatement.getDesignator().obj, designatorStatement.getExpression().struct, designatorStatement.getLine());
    }
    
    public void visitDesignStmtAssign(Obj designStmt, Struct expr, int line){
    	if(!designStmt.equals(MyTab.noObj) && !expr.equals(MyTab.noType)){
	    	if(designStmt.getKind() != Obj.Var && designStmt.getKind() != Obj.Elem){
	    		report_error("GRESKA NA LINIJI " + line + 
	    				" - operand sa leve strane dodele nije ni promenljiva ni element niza!", null);
	    		return;
	    	}
	    	if(!expr.compatibleWith(designStmt.getType())){
	    		report_error("GRESKA NA LINIJI " + line + 
	    			" - tipovi sa leve i desne strane dodele vrednosti nisu kompatibilni ", null);
	    	}
    	}
    }
    
    public void visit(DesignatorStmtIncSemi designatorStatement){
    	visitDesignStmtIncDec(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtIncParen designatorStatement){
    	visitDesignStmtIncDec(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtDecSemi designatorStatement){
    	visitDesignStmtIncDec(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtDecParen designatorStatement){
    	visitDesignStmtIncDec(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visitDesignStmtIncDec(Obj designStmt, int line){
    	if(!designStmt.equals(MyTab.noObj)){
    		if(designStmt.getKind() != Obj.Var && designStmt.getKind() != Obj.Elem){
	    		report_error("GRESKA NA LINIJI " + line + 
	    				" - operand sa leve strane dodele nije ni promenljiva ni element niza!", null);
	    		return;
	    	}
    		if(!designStmt.getType().equals(MyTab.intType)){
    			report_error("GRESKA NA LINIJI " + line + 
    	    			" - designator sa imenom " + designStmt.getName() + " mora biti tipa int!", null);
    		}
    	}
    }
    
    public void visit(DesignatorStmtFuncCallSemi designatorStatement){
    	visitDesignStmtFuncCall(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtFuncCallParen designatorStatement){
    	visitDesignStmtFuncCall(designatorStatement.getDesignator().obj, designatorStatement.getLine());
    }
    
    public void visitDesignStmtFuncCall(Obj designatorObj, int line){
    	if(!designatorObj.equals(MyTab.noObj)){
	    	if(designatorObj.getKind()!=Obj.Meth){
	    		report_error("GRESKA NA LINIJI " + line + " - simbol sa imenom "+ designatorObj.getName() +" nije metoda! ", null);
	    	} else if(designatorObj.getName().equalsIgnoreCase("len") && 
	    			currActParams.size() == 1 && 
	    			currActParams.get(0).getKind() == Struct.Array){
	    		report_info("Poziv metode " + designatorObj.getName() + " na liniji " + line,null);
	    	}else if(!parametersMatched(designatorObj)){
	    		report_error("GRESKA NA LINIJI " + line + " - nekompatibilnost aktuelnih i formalnih parametara pri pozivu metode sa imenom "+ designatorObj.getName(), null);
	    	} else{
	    		report_info("Poziv metode " + designatorObj.getName() + " na liniji " + line,null);
	    	}
    	}
    	currActParams.clear();
    }
    
    public void visit(DesignatorStmtUnionSemi designatorStatement){
    	visitDesignStmtUnion(designatorStatement.getDesignator().obj, designatorStatement.getDesignator1().obj, 
    			designatorStatement.getDesignator2().obj, designatorStatement.getLine());
    }
    
    public void visit(DesignatorStmtUnionParen designatorStatement){
    	visitDesignStmtUnion(designatorStatement.getDesignator().obj, designatorStatement.getDesignator1().obj, 
    			designatorStatement.getDesignator2().obj, designatorStatement.getLine());
    }
    
    public void visitDesignStmtUnion(Obj design1, Obj design2, Obj design3, int line){
    	if(design1.equals(MyTab.noObj) || design2.equals(MyTab.noObj) || design3.equals(MyTab.noObj)){
    		return;
    	}
    	if(!design1.getType().compatibleWith(MyTab.setType) || !design2.getType().compatibleWith(MyTab.setType) ||
    			!design3.getType().compatibleWith(MyTab.setType)){
    		report_error("GRESKA NA LINIJI " + line + " - destinaciona promenljiva i oba operanda operacije union moraju bit tipa set!", null);
    	}
    }
        
    public void visit(FactorDesign factorDesignator){
    	if(!factorDesignator.getDesignator().obj.equals(MyTab.noObj)){
    		if(factorDesignator.getDesignator().obj.getKind() == Obj.Meth){
    			report_error("GRESKA NA LINIJI " + factorDesignator.getLine() + 
	    				" - simbol sa imenom " + factorDesignator.getDesignator().obj.getName() + 
	    				" se koristi kao promenljiva, a definisan je kao funkcija", null);
    			factorDesignator.struct = MyTab.noType;
    		}
        	factorDesignator.struct = factorDesignator.getDesignator().obj.getType();
    	} else{
    		factorDesignator.struct = MyTab.noType;
    	} 	
    }
    
    public void visit (FactorFuncCall ffc){
    	Obj obj = ffc.getDesignator().obj;
    	if(!obj.equals(MyTab.noObj)){
	    	if(obj.getKind()!=Obj.Meth){
	    		report_error("GRESKA NA LINIJI " + ffc.getLine() + " - simbol sa imenom "+ obj.getName() +" nije metoda! ", null);
	    		ffc.struct = MyTab.noType;
	    	} else if(obj.getType().equals(MyTab.noType)){
	    		report_error("GRESKA NA LINIJI " + ffc.getLine() + " - metoda sa imenom "+ obj.getName() +" mora imati povratnu vrednost! ", null);
	    		ffc.struct = MyTab.noType;
	    	} else if(obj.getName().equalsIgnoreCase("len") && 
	    			currActParams.size() == 1 && 
	    			currActParams.get(0).getKind() == Struct.Array){
	    		report_info("Poziv metode " + obj.getName() + " na liniji " + ffc.getLine(),null);
	    		ffc.struct = obj.getType();
	    	}else if(!parametersMatched(obj)){
	    		report_error("GRESKA NA LINIJI " + ffc.getLine() + " - nekompatibilnost aktuelnih i formalnih parametara pri pozivu metode sa imenom "+ obj.getName(), null);
	    		ffc.struct = MyTab.noType;
	    	}else{
	    		report_info("Poziv metode " + obj.getName() + " na liniji " + ffc.getLine(),null);
	    		ffc.struct = obj.getType();
	    	}
    	} else{
    		ffc.struct = MyTab.noType;
    	}
    	currActParams.clear();
    }
    
    public void visit(FactorNum num){
    	num.struct = MyTab.intType;
    }
    
    public void visit(FactorChar character){
    	character.struct = MyTab.charType;
    }
    
    public void visit(FactorBool bool){
    	bool.struct = MyTab.boolType;
    }
    
    public void visit(FactorNewExpression newExpression){
    	if(!newExpression.getExpression().struct.equals(MyTab.noType)){
    		if(currType.getKind()==MyStruct.Set){
        		newExpression.struct = currType;
    			return;
    		}
        	if(!newExpression.getExpression().struct.compatibleWith(MyTab.intType)){
        		report_error("GRESKA NA LINIJI " + newExpression.getLine() + " - tip unutar [] mora biti int!" , null);
        		newExpression.struct = MyTab.noType;
    			return;
        	}
        	newExpression.struct = new Struct(Struct.Array, currType);
    	} else{
    		newExpression.struct = MyTab.noType;
    	}  	
    }
    
    public void visit(FactorExpr factorExpr){
    	factorExpr.struct = factorExpr.getExpression().struct;
    }
    
    public void visit(TermRecursion termRec){
    	if(termRec.getTerm().struct.equals(MyTab.noType) || termRec.getFactor().struct.equals(MyTab.noType)){
    		termRec.struct = MyTab.noType;
    		return;
    	}
    	if(!termRec.getTerm().struct.compatibleWith(MyTab.intType) || 
    			!termRec.getFactor().struct.compatibleWith(MyTab.intType)){
    		report_error("GRESKA NA LINIJI " + termRec.getLine() + " - operandi operacije MulOp moraju biti tipa int!", null);
    		termRec.struct = MyTab.noType;
    	} else{
    		termRec.struct = MyTab.intType;
    	}
    }
    
    public void visit(TermFactor termFactor){
    	termFactor.struct = termFactor.getFactor().struct;
    }
    
    public void visit(ExpressionListRecursion exprListRec){
    	if(exprListRec.getTerm().struct.equals(MyTab.noType) || exprListRec.getExpressionList().struct.equals(MyTab.noType)){
    		exprListRec.struct = MyTab.noType;
    		return;
    	}
    	if(!exprListRec.getExpressionList().struct.compatibleWith(exprListRec.getTerm().struct) || 
    			!exprListRec.getExpressionList().struct.compatibleWith(MyTab.intType) ||
    			!exprListRec.getTerm().struct.compatibleWith(MyTab.intType)){
    		report_error("GRESKA NA LINIJI " + exprListRec.getLine() + " - operandi operacije AddOp moraju biti tipa int i kompatibilni!", null);
    		exprListRec.struct = MyTab.noType;
    	} else{
    		exprListRec.struct = MyTab.intType;
    	}
    }
    
    public void visit(ExpressionListTerminal exprList){
    	exprList.struct = exprList.getTerm().struct;
    }
    
    public void visit(Expressions expr){
    	expr.struct = expr.getExpressionList().struct;
    }
    
    public void visit(ExpressionsWithStartingMinus expr){
    	if(expr.getExpressionList().struct.equals(MyTab.noType)){
    		expr.struct = MyTab.noType;
    		return;
    	}
    	if(!expr.getExpressionList().struct.equals(MyTab.intType)){
    		report_error("GRESKA NA LINIJI " + expr.getLine() + " - tip izraza iza znaka '-' mora biti int!", null);
    		expr.struct = MyTab.noType;
    		return;
    	}
    	expr.struct = expr.getExpressionList().struct;
    }
    
    public void visit(MapExpression mapExpr){
    	Obj designLeft = mapExpr.getDesignator().obj, designRight = mapExpr.getDesignator1().obj;
    	if(designLeft.equals(MyTab.noObj) || designRight.equals(MyTab.noObj)){
    		mapExpr.struct = MyTab.noType;
    		return;
    	}
    	
    	if(!(designLeft.getKind() == Obj.Meth) || !(designLeft.getLevel() == 1) || 
    			!(functionParameters.containsKey(designLeft.getName())) ||
    			!(functionParameters.get(designLeft.getName()).get(0).getType().compatibleWith(MyTab.intType)) || 
    			!(designLeft.getType().compatibleWith(MyTab.intType))){
    		report_error("GRESKA NA LINIJI " + mapExpr.getLine() + " - operand sa leve strane "
    				+ "operacije map mora predstavljati funkciju koja prima jedan parametar" + 
    					" tipa int i njena povratna vrednost je tipa int!", null);	
    		mapExpr.struct = MyTab.noType;
    	} else if(designRight.getType().getKind()!=Struct.Array || !designRight.getType().getElemType().compatibleWith(MyTab.intType)){
    		report_error("GRESKA NA LINIJI " + mapExpr.getLine() + " - operand sa desne strane "
				+ "operacije map mora predstavljati niz celobrojnih vrednosti!", null);
    		mapExpr.struct = MyTab.noType;
    	} else{
    		mapExpr.struct = MyTab.intType;
    	}
    }
    
    public void visit(EnterDoWhile enter){
    	insideDoWhile = true;
    }
    
    public void visit(ExitDoWhile exit){
    	insideDoWhile = false;
    }
    
    public void visit(BreakStatement breakStatement){
    	if(!insideDoWhile){
    		report_error("GRESKA NA LINIJI " + breakStatement.getLine() + " - break iskaz se ne nalazi unutar do-while petlje!", null);
    	}
    }
    
    public void visit(ContinueStatement continueStatement){
    	if(!insideDoWhile){
    		report_error("GRESKA NA LINIJI " + continueStatement.getLine() + " - continue iskaz se ne nalazi unutar do-while petlje!", null);
    	}
    }
    
    public void visit(ReadStatement readStatement){
    	if(readStatement.getDesignator().obj.equals(MyTab.noObj)){
    		return;
    	}
    	if(readStatement.getDesignator().obj.getKind() != Obj.Var && 
    			readStatement.getDesignator().obj.getKind() != Obj.Elem){
    		report_error("GRESKA NA LINIJI " + readStatement.getLine() + 
    				" - parametar operacije read mora biti ili promenljiva ili element niza!", null);
    		return;
    	}
    	if(!readStatement.getDesignator().obj.getType().equals(MyTab.intType) && 
    			!readStatement.getDesignator().obj.getType().equals(MyTab.charType) &&
    			!readStatement.getDesignator().obj.getType().equals(MyTab.boolType)){
    			report_error("GRESKA NA LINIJI " + readStatement.getLine() + 
    				" - parametar operacije read mora biti tipa int, char ili bool!", null);
    		return;
    	}
    }
    
    public void visit(PrintExpressionStatement printStatement){
    	visitPrintStatement(printStatement.getExpression().struct, printStatement.getLine());
    }
    
    public void visit(PrintExpressionWithNumberStatement printStatement){
    	visitPrintStatement(printStatement.getExpression().struct, printStatement.getLine());
    }
    
    public void visitPrintStatement(Struct expression, int line){
    	if(expression.equals(MyTab.noType)){
    		return;
    	}
    	if(!expression.equals(MyTab.intType) && 
    			!expression.equals(MyTab.charType) &&
    			!expression.equals(MyTab.boolType) &&
    			!expression.equals(MyTab.setType)){
    			report_error("GRESKA NA LINIJI " + line + 
    				" - parametar operacije print mora biti tipa int, char, bool ili set!", null);
    		return;
    	}
    }
    
    public void visit(EqualRelOp relOp){
    	currRelOp = "==";
    }
    
    public void visit(DiffRelOp relOp){
    	currRelOp = "!=";
    }
    
    public void visit(GrtRelOp relOp){
    	currRelOp = ">";
    }
    
    public void visit(GreRelOp relOp){
    	currRelOp = ">=";
    }
    
    public void visit(LssRelOp relOp){
    	currRelOp = "<";
    }
    
    public void visit(LsrRelOp relOp){
    	currRelOp = "<=";
    }
    
    public void visit(CondFactSingleExpr condFact){
    	if(!condFact.getExpression().struct.equals(MyTab.boolType)){
    		condFact.struct = MyTab.noType;
    		report_error("GRESKA NA LINIJI " + condFact.getLine() + " - izraz mora biti tipa bool!", null);
    	} else{
    		condFact.struct = MyTab.boolType;
    	}
    }
    
    public void visit(CondFactRelOpExpression condFact){
    	if(condFact.getExpression().struct.equals(MyTab.noType) || 
    			condFact.getExpression1().struct.equals(MyTab.noType)){
    		condFact.struct = MyTab.noType;
    		return;
    	}
    	if(!condFact.getExpression().struct.compatibleWith(condFact.getExpression1().struct)){
    		report_error("GRESKA NA LINIJI " + condFact.getLine() + " - Tipovi oba izraza moraju biti kompatibilni!", null);
    		condFact.struct = MyTab.noType;
    		return;
    	}
    	if(condFact.getExpression().struct.getKind() == Struct.Array && !currRelOp.equals("==") && !currRelOp.equals("!=")){
    		report_error("GRESKA NA LINIJI " + condFact.getLine() + " - Uz promenljive tipa niza, od relacionih operatora, mogu se koristiti samo != i ==!", null);
    		condFact.struct = MyTab.noType;
    		return;
    	}
    	condFact.struct = MyTab.boolType;   	
    }
    
    public void visit(SingleCondFact condTerm){
    	condTerm.struct = condTerm.getCondFact().struct;
    }
    
    public void visit(MultCondFact condTerm){
    	if(condTerm.getCondTerm().struct.equals(MyTab.noType) || condTerm.getCondFact().struct.equals(MyTab.noType)){
    		condTerm.struct = MyTab.noType;
    	} else{
    		condTerm.struct = MyTab.boolType;
    	}
    }
    
    public void visit(SingleCondTerm condition){
    	condition.struct = condition.getCondTerm().struct;
    }
    
    public void visit(MultCondTerm condition){
    	if(condition.getCondition().struct.equals(MyTab.noType) || condition.getCondTerm().struct.equals(MyTab.noType)){
    		condition.struct = MyTab.noType;
    	} else{
    		condition.struct = MyTab.boolType;
    	}
    }  
}