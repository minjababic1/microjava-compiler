package rs.ac.bg.etf.pp1;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Stack;

import org.apache.log4j.Logger;

import rs.ac.bg.etf.pp1.CounterVisitor.FormParamCounter;
import rs.ac.bg.etf.pp1.CounterVisitor.VarCounter;
import rs.ac.bg.etf.pp1.ast.SyntaxNode;
import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.etf.pp1.mj.runtime.*;
import rs.ac.bg.etf.pp1.ast.*;
import rs.etf.pp1.symboltable.concepts.*;

public class CodeGenerator extends VisitorAdaptor {
	
	private int mainPc;
	private boolean neg = false;
	private boolean insideParen = false;
	private Stack<Queue<Integer>> jmpBeforeAnd = new Stack<Queue<Integer>>();
	private Stack<Queue<Integer>> jmpBeforeOr = new Stack<Queue<Integer>>();
	private Stack<Integer> ifElseJmpAdr = new Stack<>();
	private Stack<Integer> doWhileJumpAdr = new Stack<>();
	private Stack<Queue<Integer>> breakDoWhileJumpAdr = new Stack<Queue<Integer>>();
	private Stack<Queue<Integer>> continueDoWhileJumpAdr = new Stack<Queue<Integer>>();
	private int printSetFooAdr = -1;
	private int setOpFooAdr = -1;
	//private int mapFooAdr = 0;
	
	Logger log = Logger.getLogger(getClass());
	
	public int getMainPc(){
		return mainPc;
	}
		
	public void visit(PrintExpressionStatement printStatement){
		visitPrintStatement(printStatement.getExpression(), 5, 1);
	}
	
	public void visit(PrintExpressionWithNumberStatement printStatement){
		visitPrintStatement(printStatement.getExpression(), printStatement.getNum(), printStatement.getNum());
	}
	
	public void visitPrintStatement(Expression expr, int intWidth, int charWidth){
		if(expr.struct.compatibleWith(MyTab.intType) || expr.struct.compatibleWith(MyTab.boolType)){
			Code.loadConst(intWidth);
			Code.put(Code.print);
		} else if(expr.struct.compatibleWith(MyTab.charType)){
			Code.loadConst(charWidth);
			Code.put(Code.bprint);
		} else{
			Code.loadConst(intWidth);
			Code.put(Code.call);
			Code.put2(printSetFooAdr - Code.pc + 1);
		}
	}
	
	public void visit(FactorNum num){
		Code.loadConst(num.getNumVal());
		if(neg == true && insideParen == false){
			Code.put(Code.neg);
			neg = false;
		}
	}
	
	public void visit(FactorChar factorChar){
		Code.loadConst((int)factorChar.getCharVal());
	}
	
	public void visit(FactorBool bool){
		Code.loadConst((bool.getBoolVal() == true) ?  1 :  0);
	}
	
	public void generateAddAllFunction(){
		// void addAll(set s, int arr[]) int i;
		Obj fooObj = MyTab.find("addAll");
		fooObj.setAdr(Code.pc);
		
		Code.put(Code.enter);
		Code.put(2);
		Code.put(3);
		// i = 0;
		Code.loadConst(0);
		Code.put(Code.store_2);
		// while(i<len(array))
		int startWhileAdr = Code.pc;
		Code.put(Code.load_2); // i
		Code.put(Code.load_1); // i, arrAdr
		Code.put(Code.arraylength); // i, arr_len
		Code.putFalseJump(Code.lt, 0);
		int retFixupAdr = Code.pc-2;
		// add(s1, arr[i])
		Code.put(Code.load_n); // s1Adr
		Code.put(Code.load_1); // s1Adr, arrAdr
		Code.put(Code.load_2); // s1Adr, arrAdr, i
		Code.put(Code.aload); // s1Adr, arr[i]
		Code.put(Code.call);
		Code.put2(MyTab.find("add").getAdr() - Code.pc + 1);
		// i = i + 1;
		Code.put(Code.load_2); // i
		Code.loadConst(1); // i, 1
		Code.put(Code.add); // i+1
		Code.put(Code.store_2);
		
		Code.putJump(startWhileAdr);
		Code.fixup(retFixupAdr);
		Code.put(Code.exit);
		Code.put(Code.return_);
		
	}
	
	public void generateAddFunction(){
		// void add(set s, int val) int i;
		Obj fooObj = MyTab.find("add");
		fooObj.setAdr(Code.pc);
		
		Queue<Integer> jumpOnRetFixupAdr = new LinkedList<>();
		
		Code.put(Code.enter);
		Code.put(2);
		Code.put(3);
		
		// i = 0;
		Code.loadConst(0);
		Code.put(Code.store_2);
		// if(setSize == len(set)){return}
		Code.put(Code.load_n); // setAdr
		Code.loadConst(0); // setAdr, 0
		Code.put(Code.aload); // set[0] -> setSize
		Code.put(Code.load_n); // setSize, setAdr
		Code.put(Code.arraylength); // setSize, setCap+1
		Code.loadConst(1); // setSize, setCap + 1, 1
		Code.put(Code.sub); // setSize, setCap
		Code.putFalseJump(Code.ne, 0);
		jumpOnRetFixupAdr.add(Code.pc-2);
		// while(i<setSize)
		int startWhileAdr = Code.pc;
		Code.put(Code.load_2); // i
		Code.put(Code.load_n); // i, setAdr
		Code.loadConst(0); // i, setAdr, 0
		Code.put(Code.aload); // i, setSize 
		Code.putFalseJump(Code.lt, 0);
		int jumpOnEndWhileFixupAdr = Code.pc-2;
		// if(set[i] == val){return;}
		Code.put(Code.load_n); // setAdr
		Code.put(Code.load_2); // setAdr, i
		Code.loadConst(1); // setAdr, i, 1
		Code.put(Code.add); // setAdr, i+1
		Code.put(Code.aload); // set[i+1]
		Code.put(Code.load_1); // set[i+1], val
		Code.putFalseJump(Code.ne, 0);
		jumpOnRetFixupAdr.add(Code.pc-2);
		// i++;
		Code.put(Code.load_2); // i
		Code.loadConst(1); // i, 1
		Code.put(Code.add); // i+1
		Code.put(Code.store_2); // i = i+1
		Code.putJump(startWhileAdr);
		Code.fixup(jumpOnEndWhileFixupAdr);
		// set[setSize+1] = val
		Code.put(Code.load_n); // setAdr
		Code.put(Code.load_n); // setAdr, setAdr
		Code.loadConst(0); // setAdr, setAdr, 0
		Code.put(Code.aload); // setAdr, setSize
		Code.loadConst(1); // setAdr, setSize, 1
		Code.put(Code.add); // setAdr, setSize+1
		Code.put(Code.load_1); // setAdr, setSize + 1, val
		Code.put(Code.astore); // set[setSize+1] = val
		// setSize++; -> set[0] = set[0] + 1;
		Code.put(Code.load_n); // setAdr
		Code.loadConst(0);  // setAdr, 0
		Code.put(Code.load_n); // setAdr, 0, setAdr
		Code.loadConst(0); // setAdr, 0, setAdr, 0
		Code.put(Code.aload); // setAdr, 0, set[0]
		Code.loadConst(1); // setAdr, 0, set[0], 1
		Code.put(Code.add); // setAdr, 0, set[0] + 1
		Code.put(Code.astore); //  set[0] = set[0] + 1
		while(jumpOnRetFixupAdr.size() > 0){
			Code.fixup(jumpOnRetFixupAdr.remove());
		}
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void generateSetOpFunction(){
		// Set Setop(set s1, set s2, s3) int i
		setOpFooAdr = Code.pc;
		Code.put(Code.enter);
		Code.put(3);
		Code.put(4);
		
		// i = 0
		Code.loadConst(0);
		Code.put(Code.store_3);
		// while( i < s2.size)
		int whileAdr = Code.pc;
		Code.put(Code.load_3); // i
		Code.put(Code.load_1); // i, s2
		Code.loadConst(0); // i, s2, 0
		Code.put(Code.aload); // i, s2.size()
		Code.putFalseJump(Code.lt, 0);
		int whileExitFixupAdr = Code.pc-2;
		// add(s1, s2[i+1]);
		Code.put(Code.load_n); // s1
		Code.put(Code.load_1); // s1, s2
		Code.put(Code.load_3); // s1, s2, i
		Code.loadConst(1); // s1, s2, i, 1
		Code.put(Code.add); // s1, s2, i+1
		Code.put(Code.aload); // s1, s2[i+1]
		Code.put(Code.call);
		Code.put2(MyTab.find("add").getAdr() - Code.pc + 1);
		// i = i + 1
		Code.put(Code.load_3); // i
		Code.loadConst(1); // i, 1
		Code.put(Code.add); // i+1
		Code.put(Code.store_3);
		
		Code.putJump(whileAdr);
		Code.fixup(whileExitFixupAdr);
		
		// i = 0
		Code.loadConst(0);
		Code.put(Code.store_3);
		// while( i < s3.size)
		whileAdr = Code.pc;
		Code.put(Code.load_3); // i
		Code.put(Code.load_2); // i, s3
		Code.loadConst(0); // i, s3, 0
		Code.put(Code.aload); // i, s3.size()
		Code.putFalseJump(Code.lt, 0);
		whileExitFixupAdr = Code.pc-2;
		// add(s1, s3[i+1]);
		Code.put(Code.load_n); // s1
		Code.put(Code.load_2); // s1, s3
		Code.put(Code.load_3); // s1, s3, i
		Code.loadConst(1); // s1, s3, i, 1
		Code.put(Code.add); // s1, s3, i+1
		Code.put(Code.aload); // s1, s3[i+1]
		Code.put(Code.call);
		Code.put2(MyTab.find("add").getAdr() - Code.pc + 1);
		// i = i + 1
		Code.put(Code.load_3); // i
		Code.loadConst(1); // i, 1
		Code.put(Code.add); // i+1
		Code.put(Code.store_3);
		
		Code.putJump(whileAdr);
		Code.fixup(whileExitFixupAdr);
		
		Code.put(Code.load_n);
		
		Code.put(Code.exit);
		Code.put(Code.return_);
		
		
	}
	
	public void generatePrintSetFoo(){
		// void printSetFoo(set s, int width) int i;
		printSetFooAdr = Code.pc;
		Code.put(Code.enter);
		Code.put(2);
		Code.put(3);
		
		// i = 0;
		Code.loadConst(0);
		Code.put(Code.store_2);
		// while( i < setSize)
		int whileAdr = Code.pc;
		Code.put(Code.load_2); // i
		Code.put(Code.load_n); // i, setAdr
		Code.loadConst(0); // i, setAdr, 0
		Code.put(Code.aload); // i, setSize
		Code.putFalseJump(Code.lt, 0);
		int retFixupAdr = Code.pc-2;
		// print(set[i+1])
		Code.put(Code.load_n); // setAdr
		Code.put(Code.load_2); // setAdr, i
		Code.loadConst(1); // setAdr, i, 1
		Code.put(Code.add); // setAdr, i+1
		Code.put(Code.aload); // set[i+1];
		Code.put(Code.load_1); // set[i+1], width;
		Code.put(Code.print); // print(set[i+1], width);
		Code.loadConst(32);
		Code.loadConst(1);
		Code.put(Code.bprint);
		// i = i+1
		Code.put(Code.load_2);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.put(Code.store_2);
		Code.putJump(whileAdr);
		Code.fixup(retFixupAdr);
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ProgName progName){
		generateAddFunction();
		generateAddAllFunction();
		generateSetOpFunction();
		generatePrintSetFoo();
	}
	
	public void visit(MethodTypeAndName method){
		if("main".equalsIgnoreCase(method.getMethodName())){
			mainPc = Code.pc;
		}
		method.obj.setAdr(Code.pc);
		
		SyntaxNode methodNode = method.getParent();
		
		FormParamCounter formalParamCnt = new FormParamCounter();
		methodNode.traverseTopDown(formalParamCnt);
		
		VarCounter varCounter = new VarCounter();
		methodNode.getParent().traverseTopDown(varCounter);
		
		Code.put(Code.enter);
		Code.put(formalParamCnt.getCount());
		Code.put(formalParamCnt.getCount() + varCounter.getCount());		
	}
	
	public void visit(MethodDecl methodDecl){
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(DesignatorStmtAssignSemi designatorStatement){
		Code.store(designatorStatement.getDesignator().obj);
	}
	
	public void visit(DesignatorStmtAssignParen designatorStatement){
		Code.store(designatorStatement.getDesignator().obj);
	}
	
	public void visit(FactorDesign factorDesign){
		Code.load(factorDesign.getDesignator().obj);
		if(neg == true && insideParen == false){
			Code.put(Code.neg);
			neg = false;
		}
	}
	
	public void visit(TermRecursion termRec){
		if(termRec.getMulOp() instanceof MulMulOp){
			Code.put(Code.mul);
		} else if(termRec.getMulOp() instanceof DivMulOp){
			Code.put(Code.div);
		} else{
			Code.put(Code.rem);
		}
	}
	
	public void visit(ExpressionListRecursion exprListRec){
		if(exprListRec.getAddOp() instanceof PlusAddOp){
			Code.put(Code.add);
		} else{
			Code.put(Code.sub);
		}
	}
	
	public void visit(EnterExprWithStartingMinus exprList){
		neg = true;
	}
	
	public void visit(EnterParenExpression enterParenExpr){
		insideParen = true;
	}
	
	public void visit(FactorExpr factorExpr){
		if(neg == true){
			Code.put(Code.neg);
			neg = false;
		}
		insideParen = false;
	}
	
	public void visit(DesignatorStmtIncSemi inc){
		incrementDesign(inc.getDesignator().obj);
	}
	
	public void visit(DesignatorStmtIncParen inc){
		incrementDesign(inc.getDesignator().obj);
	}
	
	public void incrementDesign(Obj designObj){
		if(designObj.getKind() == Obj.Elem){
			Code.put(Code.dup2);
		}
		Code.load(designObj);
		Code.loadConst(1);
		Code.put(Code.add);
		Code.store(designObj);
	}
	
	public void visit(DesignatorStmtDecSemi dec){
		decrementDesign(dec.getDesignator().obj);
	}
	
	public void visit(DesignatorStmtDecParen dec){
		decrementDesign(dec.getDesignator().obj);
	}
	
	public void decrementDesign(Obj designObj){
		if(designObj.getKind() == Obj.Elem){
			Code.put(Code.dup2);
		}
		Code.load(designObj);
		Code.loadConst(1);
		Code.put(Code.sub);
		Code.store(designObj);
	}
	
	public void visit(ReadStatement readStmt){
		if(readStmt.getDesignator().obj.getType().compatibleWith(MyTab.charType)){
			Code.put(Code.bread);
		} else{
			Code.put(Code.read);
		}
		Code.store(readStmt.getDesignator().obj);
	}
	
	public void visit(FactorNewExpression factorNewExpr){
		if(factorNewExpr.getType().struct.compatibleWith(MyTab.setType)){
			Code.loadConst(1);
			Code.put(Code.add);
		}
		Code.put(Code.newarray);
		if(factorNewExpr.getType().struct.compatibleWith(MyTab.charType)){
			Code.put(0);
		} else{
			Code.put(1);
		}	
	}
	
	public void visit(EnterDesignatorArray enterDesignArr){
		Code.load(enterDesignArr.obj);
	}
	
	public void visit(FactorFuncCall ffc){
		String fooName = ffc.getDesignator().obj.getName();
		if(fooName.equalsIgnoreCase("len")){
			Code.put(Code.arraylength);
		} else if(fooName.equalsIgnoreCase("ord") || fooName.equalsIgnoreCase("chr")){
			return;
		} else{
			Code.put(Code.call);
			Code.put2(ffc.getDesignator().obj.getAdr() - Code.pc + 1);
		}
	}
	
	public void visit(DesignatorStmtFuncCallSemi designStmt){
		visitDesignStmtFuncCall(designStmt.getDesignator().obj);
	}
	
	public void visit(DesignatorStmtFuncCallParen designStmt){
		visitDesignStmtFuncCall(designStmt.getDesignator().obj);
	}
	
	public void visitDesignStmtFuncCall(Obj functionObj){
		String fooName = functionObj.getName();
		if(fooName.equalsIgnoreCase("len")){
			Code.put(Code.arraylength);
		} else if(fooName.equalsIgnoreCase("ord") || fooName.equalsIgnoreCase("chr")){
			return;
		} else{
			Code.put(Code.call);
			Code.put2(functionObj.getAdr() - Code.pc + 1);
			if(!functionObj.getType().equals(MyTab.noType)){
				Code.put(Code.pop);
			}
		}
	}
	
	public void visit(SingleCondFact condTermToCondFact){
		generateJumps(condTermToCondFact, condTermToCondFact.getCondFact());
	}
	
	public void visit(MultCondFact condTermAndCondFact){
		generateJumps(condTermAndCondFact, condTermAndCondFact.getCondFact());
	}
	
	public void generateJumps(CondTerm condTerm, CondFact condFact){
		if(condTerm.getParent() instanceof MultCondFact){
			// Nakon ovog expression-a ide &&
			conditionJump(condFact, Code.eq, Code.ne, Code.gt, Code.ge, Code.lt, Code.le, Code.ne);
			jmpBeforeAnd.peek().add(Code.pc-2);
		} else if(condTerm.getParent() instanceof Condition && 
				!(condTerm.getParent().getParent() instanceof Condition)){
			// Poslednji uslov u condition-u
			conditionJump(condFact, Code.eq, Code.ne, Code.gt, Code.ge, Code.lt, Code.le, Code.ne);
			jmpBeforeAnd.peek().add(Code.pc-2);
			fixupAddresses(jmpBeforeOr.peek());
		} else if(condTerm.getParent() instanceof SingleCondTerm && 
				condTerm.getParent().getParent() instanceof MultCondTerm){
			conditionJump(condFact, 
					Code.inverse[Code.eq], Code.inverse[Code.ne], Code.inverse[Code.gt], 
					Code.inverse[Code.ge], Code.inverse[Code.lt], Code.inverse[Code.le], Code.inverse[Code.ne]);
			jmpBeforeOr.peek().add(Code.pc-2);
			// fixup dest adr for expr that come after && 
			fixupAddresses(jmpBeforeAnd.peek());
		}
	}

	private void conditionJump(CondFact condFact, int eq, int ne, int gt, int ge, int lt, int le, int comp) {
		if(condFact instanceof CondFactSingleExpr){
			Code.loadConst(0);
			Code.putFalseJump(comp, 0);
		} else{
			CondFactRelOpExpression relOpExpr = (CondFactRelOpExpression)condFact;
			if(relOpExpr.getRelOp() instanceof EqualRelOp){
				Code.putFalseJump(eq, 0);
			} else if(relOpExpr.getRelOp() instanceof DiffRelOp){
				Code.putFalseJump(ne, 0);
			} else if(relOpExpr.getRelOp() instanceof GrtRelOp){
				Code.putFalseJump(gt, 0);
			} else if(relOpExpr.getRelOp() instanceof GreRelOp){
				Code.putFalseJump(ge, 0);
			} else if(relOpExpr.getRelOp() instanceof LssRelOp){
				Code.putFalseJump(lt, 0);
			} else if(relOpExpr.getRelOp() instanceof LsrRelOp){
				Code.putFalseJump(le, 0);
			}
		}
	}
	
	public void fixupAddresses(Queue<Integer> queue){
		if(queue == null){
			return;
		}
		while(queue.size() > 0){
			int patchAdr = queue.remove();
			Code.fixup(patchAdr);
		}
	}
	
	public void visit(EnterIf enterIf){
		jmpBeforeAnd.add(new LinkedList<Integer>());
		jmpBeforeOr.add(new LinkedList<Integer>());
	}
	
	public void visit(IfElseStatementIf stmt){
		fixupAddresses(jmpBeforeAnd.peek());
		jmpBeforeAnd.pop();
		jmpBeforeOr.pop();
	}
	
	public void visit(EnterElse stmt){
		Code.putJump(0);
		ifElseJmpAdr.push(Code.pc-2);
		fixupAddresses(jmpBeforeAnd.peek());
	}
	
	public void visit(IfElseStatementIfElse ifStatement){
		Code.fixup(ifElseJmpAdr.pop());
		jmpBeforeAnd.pop();
		jmpBeforeOr.pop();
	}
	
	public void visit(EnterDoWhile enterDoWhile){
		jmpBeforeAnd.add(new LinkedList<Integer>());
		jmpBeforeOr.add(new LinkedList<Integer>());
		breakDoWhileJumpAdr.add(new LinkedList<Integer>());
		continueDoWhileJumpAdr.add(new LinkedList<Integer>());
		doWhileJumpAdr.push(Code.pc);
	}
	
	public void visit(JumpOnDo jumpOnDo){
		Code.putJump(doWhileJumpAdr.pop());
		fixupAddresses(breakDoWhileJumpAdr.peek());
		fixupAddresses(jmpBeforeAnd.peek());
		jmpBeforeAnd.pop();
		jmpBeforeOr.pop();
		breakDoWhileJumpAdr.pop();
	}
	
	public void visit(BreakStatement breakStmt){
		Code.putJump(0);
		breakDoWhileJumpAdr.peek().add(Code.pc-2);
	}
	
	public void visit(ContinueStatement contStmt){
		Code.putJump(0);
		continueDoWhileJumpAdr.peek().add(Code.pc-2);
	}
	
	public void visit(ExitDoWhile exitDoWhile){
		fixupAddresses(continueDoWhileJumpAdr.peek());
		continueDoWhileJumpAdr.pop();
	}
	
	
	public void visit(MapExpression mapExpr){
		// mapFooSum = 0;
		Code.loadConst(0);
		Code.put(Code.putstatic);
		Code.put2(0);
		// mapFooIndex = 0;
		Code.loadConst(0);
		Code.put(Code.putstatic);
		Code.put2(1);
		// while( mapFooIndex < len(arr) )
		int loopJumpAdr = Code.pc;
		Code.put(Code.getstatic);
		Code.put2(1); // i
		Code.load(mapExpr.getDesignator1().obj); // i, arr
		Code.put(Code.arraylength); // i, arr_len
		Code.putFalseJump(Code.lt, 0);
		int endLoopJumpAdr = Code.pc-2;
		// mapFooSum = mapFooSum + foo(arr[i]);
		Code.put(Code.getstatic);
		Code.put2(0); // sum
		Code.load(mapExpr.getDesignator1().obj); // sum, arr
		Code.put(Code.getstatic);
		Code.put2(1); // sum, arr, i
		Code.put(Code.aload); // sum, arr[i];
		Code.put(Code.call);
		Code.put2(mapExpr.getDesignator().obj.getAdr() - Code.pc + 1); // ... foo(arr[i]); -> sum, retVal
		Code.put(Code.add); // sum+retVal
		Code.put(Code.putstatic);
		Code.put2(0); // sum = sum + retVal
		Code.put(Code.getstatic);
		Code.put2(1); // i
		Code.loadConst(1);
		Code.put(Code.add); // i+1
		Code.put(Code.putstatic);
		Code.put2(1); // i = i+1
		Code.putJump(loopJumpAdr);
		Code.fixup(endLoopJumpAdr);
		Code.put(Code.getstatic);
		Code.put2(0);
	}
	
	public void visit(ReturnStatement retStmt){
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(ReturnExpressionStatement retStmt){
		Code.put(Code.exit);
		Code.put(Code.return_);
	}
	
	public void visit(DesignatorStmtUnionSemi designStmt){
		visitDesignatorStatementUnion(designStmt.getDesignator().obj, designStmt.getDesignator1().obj, designStmt.getDesignator2().obj);
	}
	
	public void visit(DesignatorStmtUnionParen designStmt){
		visitDesignatorStatementUnion(designStmt.getDesignator().obj, designStmt.getDesignator1().obj, designStmt.getDesignator2().obj);
	}
	
	public void visitDesignatorStatementUnion(Obj set1, Obj set2, Obj set3){
		Code.load(set1);
		Code.load(set2);
		Code.load(set3);
		Code.put(Code.call);
		Code.put2(setOpFooAdr - Code.pc + 1);
		Code.store(set1);
	}
	
}
