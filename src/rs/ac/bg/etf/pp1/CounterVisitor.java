package rs.ac.bg.etf.pp1;

import rs.ac.bg.etf.pp1.ast.VisitorAdaptor;
import rs.ac.bg.etf.pp1.ast.*;

public class CounterVisitor extends VisitorAdaptor {
	protected int count;
	
	public int getCount(){
		return count;
	}
	
	public static class FormParamCounter extends CounterVisitor{
	
		public void visit(FormParamLastDecl formParam){
			count++;
		}
		
		public void visit(FormParamNonLastDecl formParam){
			count++;
		}
	}
	
	public static class VarCounter extends CounterVisitor{
		
		public void visit(VarLastDecl var){
			count++;
		}
		
		public void visit(VarNonLastDecl var){
			count++;
		}
	}

}
