package rs.ac.bg.etf.pp1;

import rs.etf.pp1.symboltable.*;
import rs.etf.pp1.symboltable.concepts.*;
import rs.etf.pp1.symboltable.visitors.SymbolTableVisitor;;

public class MyTab extends Tab{
	private static int currentLevel;
	public static final Struct boolType = new Struct(Struct.Bool);
	public static final Struct setType = new MyStruct(MyStruct.Set);
	public static Obj addObj, addAllObj, printSetFooObj;
	
	public static void init() {
		Tab.init();
		currentScope.addToLocals(new Obj(Obj.Type, "bool", boolType));
		currentScope.addToLocals(new Obj(Obj.Type, "set", setType));
		currentScope.addToLocals(addObj = new Obj(Obj.Meth, "add", noType, 0, 2));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "a", setType));
			currentScope.addToLocals(new Obj(Obj.Var, "b", intType));
			addObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		currentScope.addToLocals(addAllObj = new Obj(Obj.Meth, "addAll", noType, 0, 2));
		{
			openScope();
			currentScope.addToLocals(new Obj(Obj.Var, "a", setType));
			currentScope.addToLocals(new Obj(Obj.Var, "b", new Struct(Struct.Array, intType)));
			addAllObj.setLocals(currentScope.getLocals());
			closeScope();
		}
		
		// Check if this is necessary
		
		
		// ili 
		
		//insert(Obj.Meth, "mapFooName", intType);
		//insert()
		
		currentLevel = -1;
		
	}
	
	public static void dump(SymbolTableVisitor stv) {
		System.out.println("=====================SYMBOL TABLE DUMP=========================");
		if (stv == null)
			stv = new MySTV();
		for (Scope s = currentScope; s != null; s = s.getOuter()) {
			s.accept(stv);
		}
		System.out.println(stv.getOutput());
	}
	
	public static Obj findInCurrentScope(String name){
		Obj resultObj = null;
		if (currentScope.getLocals() != null) {
			resultObj = currentScope.getLocals().searchKey(name);
		}
		return (resultObj != null) ? resultObj : noObj;
	}
	
	public static void dump() {
		dump(null);
	}
}
