package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.concepts.Struct;
import rs.etf.pp1.symboltable.visitors.DumpSymbolTableVisitor;

public class MySTV extends DumpSymbolTableVisitor {
	@Override
	public void visitStructNode(Struct structToVisit) {
		switch(structToVisit.getKind()){
			case Struct.Bool:
				output.append("bool");
				break;
			case MyStruct.Set:
				output.append("set");
			default:
				if(structToVisit.getKind()==Struct.Array && structToVisit.getElemType().getKind()==Struct.Bool){
					output.append("Arr of bool");
				} else{
					super.visitStructNode(structToVisit);
				}
			
		}
	}
}
