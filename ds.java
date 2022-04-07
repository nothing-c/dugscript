/*
  DugScript Interpreter v1.0
*/

/* Imports */
import java.lang.*;
import java.util.*;
import java.nio.file.*;
import java.io.*;

public class ds {
    
    /* Constants */
    /* 	   
	   Max size of list of operands - needs to be based on max number
	   a function needs to be passed, but is arbitrarily large for now
    */
    public static final int MAX_SIZE = 50;
    /* Initialize variable list */
    public static List<mvariable> vars = new ArrayList<mvariable>();
    /* Initialize function list */
    public static List<mfunc> funcs = new ArrayList<mfunc>();
    /* Dummy function for appeasing the JVM gods */
    public static String[] tmp = new String[1];
    public static mfunc err = new mfunc(tmp, 0, "Notfunc", tmp);
    
    /* Objects and subroutines */
    private static String eval(String[] cmds) {
	/* Evaluate statement */
	/* I wish I could have multiple different types in an array... */
	String[] stack = new String[MAX_SIZE];
	/* Keep track of where we are */
	int i = 0;
	/* Flag for signaling if we're in an if-statement */
	int intern = 0;
	List<String> terncmds = new ArrayList<String>();
	for (String s : cmds) {
	    if (s == null) break;
	    /* Handling for if-statements */
	    /* 
	       Keep skipping over the command until the if-statement's done
	    */
	    if (intern > 0 && !(s.equals(";"))) {
		terncmds.add(s);
		continue;
	    }
	    if (intern > 0 && s.equals(";")) {
		terncmds.add(s);
		stack[i] = ifeval(terncmds);
		intern = 0;
	    }
		
	    /* Handle user-defined functions */
	    if (!(funcsearch(s).name().equals("Notfunc"))) {
		/* Nab function object */
		mfunc tmp = funcsearch(s);
		/* Create new array to pass to tmp.call() */
		String[] params = new String[tmp.params()];
		/* Move i back */
		i -= tmp.params();
		/* Add params to array */
		for (int x = 0; x < tmp.params(); x++) {
		    //System.out.println("Param: " + stack[i + x]);
		    params[x] = stack[i + x];
		}
		/* Evaluate and add to stack */
		stack[i] = eval(tmp.call(params));
		/* Forgetting this caused me so much trouble */
		break;
	    }
	    switch (s) {
		/* All the math can fall through */
	    case "+":
	    case "-":
	    case "*":
	    case "/":
	    case "%":
		/* Standard RPN math */
		i -= 2;
	    if (!(search(stack[i]).equals("Notvar"))) {
		stack[i] = search(stack[i]);
	    } else if (!(search(stack[i + 1]).equals("Notvar"))) {
		stack[i + 1] = search(stack[i + 1]);	    
	    }
	    stack[i] = Double.toString(matheval(stack[i], stack[i + 1], s));
	    break;
	    case ">":
		i -= 2;
		stack[i] = numcompare(stack[i], stack[i + 1], "greater");
		break;
	    case "<":
		i -= 2;
		stack[i] = numcompare(stack[i], stack[i + 1], "lesser");
		break;
	    case ">=":
		i -= 2;
		stack[i] = numcompare(stack[i], stack[i + 1], "greatereq");
		break;
	    case "<=":
		i -= 2;
		stack[i] = numcompare(stack[i], stack[i + 1], "lessereq");
		break;
	    case "==":
		i -= 2;
		if (!(stack[i].equals(stack[i + 1]))) {
		    stack[i] = "f";
		} else {
		    stack[i] = "t";
		}
		break;
	    case "!=":
		i -= 2;
		if (stack[i].equals(stack[i + 1])) {
		    stack[i] = "f";
		} else {
		    stack[i] = "t";
		}
		break;
	    case "set":
		/* Set variables */
		i -= 2;
		if (!(search(stack[i]).equals("Notvar"))) {
		    updatevar(stack[i], stack[i + 1]);
		} else {
		    mvariable tmp = new mvariable(stack[i], stack[i + 1]);
		    stack[i] = tmp.name();
		    vars.add(tmp);
		}
		break;
	    case "defun":
		/* Define a function */
		i -= 3;
		/* 
		   This also doesn't need to return, unless I add
		   lambdas...
		   In that case, it'd just be stack[i] = funcdef();
		*/
		mfunc placeholder = funcdef(stack[i], stack[i + 1], stack[i + 2]);
		/* Get func name off of stack */
		i--;
		break;
	    case "concat":
		/* Concatenate strings */
		i -= 2; 
		/* 
		   If there are no errors, replace; otherwise, proceed as 
		   normal
		*/
		if (!(search(stack[i]).equals("Notvar"))) {
		    stack[i] = search(stack[i]);
		} else if (!(search(stack[i + 1]).equals("Notvar"))) {
		    stack[i + 1] = search(stack[i + 1]);	    
		}
		stack[i] = stack[i] + stack[i + 1];
		break;
	    case "split":
		/* Split a string into an array */
		i -= 2;
		if (!(search(stack[i]).equals("Notvar"))) {
		    stack[i] = search(stack[i]);
		} else if (!(search(stack[i + 1]).equals("Notvar"))) {
		    stack[i + 1] = search(stack[i + 1]);	    
		}
		if (stack[i + 1].equals("s")) {
		    stack[i + 1] = " ";
		}
		String[] splittmp = stack[i].split(stack[i+1]);
		stack[i] = "";
		int x = 0;
		for (String t : splittmp) {
		    /* Keep the extraneous comma from appearing */
		    if (x < splittmp.length - 1) {
			stack[i] += t + ",";
		    } else {
			stack[i] += t;
		    }
		    x++;
		}
		break;
	    case "replace":
		/* Regex search and replace */
		i -= 3;
		/* Allow replacing inside vars */
		if (!(search(stack[i]).equals("Notvar"))) {
		    stack[i] = search(stack[i]);	    
		}
		String ret = stack[i].replace(stack[i + 1], stack[i + 2]);
		stack[i] = ret;
		break;
	    case "print":
		/* 
		   Kind of redundant in the REPL, but useful once it's 
		   in a script file
		*/
		i -= 1;
		if (!(search(stack[i]).equals("Notvar"))) {
		    System.out.print(search(stack[i]));
		} else {
		    System.out.print(stack[i]);
		}
		break;
	    case "read":
		/* Read from file */
		i -= 2;
		mvariable file = new mvariable(stack[i], slurp(stack[i + 1]));
		vars.add(file);
		/* 
		   stack[i] is already the variable name, so we don't
		   need to do anything with it
		*/
		break;
	    case "write":
		/* Write to file */
		i -= 2;
		/* Nab variable contents */
		if (!(search(stack[i]).equals("Notvar"))) {
		    stack[i] = search(stack[i]);
		} else if (!(search(stack[i + 1]).equals("Notvar"))) {
		    stack[i + 1] = search(stack[i + 1]);	    
		}
		writeout(stack[i + 1], stack[i]);
		break;
	    case "sys":
		i -= 2;
		mvariable result = new mvariable(stack[i], syscall(stack[i + 1]));
		vars.add(result);
		break;
	    case "?":
		/* 
		   This just sets the flag, a lot of actual handling
		   happens up top
		*/
		/* Set flag */
		intern = 1;
		i -= 1;
		/* Set t/f flag */
		terncmds.add(stack[i]);
		break;
	    case "for":
		i -= 2;
		/* Eval each element of array separately */
		if (!(search(stack[i]).equals("Notvar"))) {
		    stack[i] = search(stack[i]);
		}
		/* Foreval doesn't need to return anything */
		foreval(stack[i], stack[i + 1]);
		i--;
		break;
	    case "quit":
		System.exit(1);
	    default:
		/* Append to stack */
		stack[i] = s;
	    }
	    i++;
	}
	return stack[0];
    }

    /* Number Mangling */
    private static Double matheval(String num1, String num2, String op) {
	/* I could do this shorter, but I'd like the code to be readable */
	Double num1_2 = Double.parseDouble(num1);
	Double num2_2 = Double.parseDouble(num2);
	switch (op) {
	case "+":
	    return (num1_2 + num2_2);
	case "-":
	    return (num1_2 - num2_2);
	case "*":
	    return (num1_2 * num2_2);
	case "/":
	    return (num1_2 / num2_2);
	case "%":
	    return (num1_2 % num2_2);
	}
	/* Bad juju here */
	return Double.parseDouble("-1");
    }

    private static String numcompare(String num1, String num2, String method) {
	/* Test for greater/lesser */
	Double first = Double.parseDouble(num1);
	Double sec = Double.parseDouble(num2);
	switch (method) {
	case "greater":
	    if (first > sec) {
		return "t";
	    } else {
		return "f";
	    }
	case "lesser":
	    if (first < sec) {
		return "t";
	    } else {
		return "f";
	    }
	case "greatereq":
	    if (first >= sec) {
		return "t";
	    } else {
		return "f";
	    }
	case "lessereq":
	    if (first <= sec) {
		return "t";
	    } else {
		return "f";
	    }
	default:
	    return "error";
	}
    }


    /* Variable Mangling */
    private static String search(String varname) {
	/* Search for variable */
	/* Check if it's an array access */
	if (varname.contains("[")) {
	    String[] tmp0 = varname.split("]");
	    varname = tmp0[0];
	}
	    
	for (mvariable v : vars) {
	    if (v.name().equals(varname)) {
		/* 
		   I think this technically copies it, instead of
		   returning a pointer...
		*/
		return v.value();
	    } else if (varname.contains(v.name())) {
		String[] tmp = varname.split("\\[");
		String tmp2 = access(v.value(), Integer.parseInt(tmp[1]));
		return tmp2;
	    }
	}
	return "Notvar";
    }

    private static void updatevar(String varname, String newval) {
	int arrflag = 0;
	/* Check if it's an array access */
	if (varname.contains("[")) {
	    String[] tmp0 = varname.split("]");
	    varname = tmp0[0];
	    arrflag = 1;
	}

	/* Very similar to search() */
	for (mvariable v : vars) {
	    /* Check if we're updating an array */
	    if (varname.contains(v.name()) && (arrflag > 0)) {
		/* Yes, this is overloaded. Check the class */
		/* Unnecessary step, but I like the code to be readable */
		String arrindex = varname.split("\\[")[1];
		v.update(newval, Integer.parseInt(arrindex));
	    } else if (v.name().equals(varname)) {
		/* If we don't have an array, just update the variable */
		v.update(newval);
	    }
	}
    }

    private static String access(String val, int index) {
	/* Access part of array */
	String[] arr = val.split(",");
	return arr[index];
    }

    /* Function Mangling */
    private static mfunc funcdef(String body, String params, String funcname) {
	/* Wrangle parameters into a usable state */
	String[] paramarr = params.split(",");
	int nparams = paramarr.length;
	String[] newcmds = body.split(" ");
	String[] revnewcmds = strcheck(newcmds);
	/* Initialize new function */
	mfunc newfunc = new mfunc(revnewcmds, nparams, funcname, paramarr);
	/* Add to list */
	funcs.add(newfunc);
	return newfunc;
    }

    private static mfunc funcsearch(String name) {
	/* Search for user-defined functions */
	for (mfunc func : funcs) {
	    if (func.name().equals(name)) {
		return func;
	    }
	}
	return err;
    }


    /* I/O Area */
    private static String slurp(String file) {
	/* 
	   Read a file into a string
	   If you want to iterate over lines from a file,
	   tough cookies. Use awk or perl
	*/
	String ret = "";
	if (file.equals("stdin")) {
	    /* Needs to be pipe-compatible */
	    file = "System.in";
	}
	/* 
	   This is stupid, but apparently idiomatic
	   I guess there's a reason why Java isn't popular for text mangling 
	*/
	/* This defaults to UTF-8 */
	Path path = Paths.get(file);
	/* This is stupid */
	try {
	    BufferedReader s = Files.newBufferedReader(path);
	    String line;
	    /* C-type getchar() kinda thing here */
	    while ((line = s.readLine()) != null) {
		ret += line + "\n";
	    }
	} catch (IOException e) {
	    e.printStackTrace();
	}
	return ret;
    }

    private static void writeout(String file, String content) {
	/* 
	   Write string to file 
	   This automagically appends, instead of overwriting, because I
	   couldn't be bothered to make an option for that	 
	*/
	/* Second verse, same as the first */
	Path path = Paths.get(file);
	int len = content.length();
	/* The options have to be done this way... */
	OpenOption[] opts = {StandardOpenOption.WRITE, StandardOpenOption.APPEND, StandardOpenOption.CREATE};
	/* Supposedly I could just do this with path.write(), but w/e */
	try {
	    /* Also defaults to UTF-8 */
	    BufferedWriter w = Files.newBufferedWriter(path, opts);
	    w.write(content, 0, len);
	    w.flush();
	} catch (IOException e) {
	    e.printStackTrace();
	}
    }

    /* System Calls */
    private static String syscall(String call) {
	/* Run syscall, return stdout */
	String ret = "";
	try {
	    Process proc = Runtime.getRuntime().exec(call);
	    /* Nab stdout */
	    InputStream stdout = proc.getInputStream();
	    BufferedReader read = new BufferedReader(new InputStreamReader(stdout));
	    String line;
	    while ((line = read.readLine()) != null ) {
		ret += line + "\n";
	    }
	    read.close();
	} catch (IOException e) {
	    e.printStackTrace();
	    ret = "An error occurred";
	}
	return ret;

    }

    /* If-statements */
    private static String ifeval(List<String> statements) {
	List<String> tmplist = new ArrayList<String>();
	/* 
	   If you want to know why I didn't use an iterator:
	   It's easier to do it this way and skip the t/f flag
	*/
	if (statements.get(0).equals("t")) {
	    for (int i = 1; i < statements.size(); i++) {
		if (statements.get(i).equals(":")) {
		    /* Stop */
		    break;
		} else if (statements.get(i).equals("?")) {
		    /* Do nothing */
		} else {
		    tmplist.add(statements.get(i));
		}
	    }
	} else {
	    /* So we know if we're adding to the eval stack */
	    int flag = 0;
	    for (int i = 1; i < statements.size(); i++) {
		if (flag == 1) {
		    tmplist.add(statements.get(i));
		}
		/* Switch flag */
		if (statements.get(i).equals(":")) {
		    flag = 1;
		}
	    }
	}
	String[] strippedlist = new String[tmplist.size()];
	/* Turn arraylist into regular array */
	for (int i = 0; i < tmplist.size(); i++) {
	    strippedlist[i] = tmplist.get(i);
	}
	/* Evaluate and return */
	String ret = eval(strippedlist);
	return ret;	
    }

    /* Loops */
    private static void foreval(String arr, String cmds) {
	String[] arraccess = arr.split(",");
	for (String str : arraccess) {
	    /* Yes, this is stupid and wastes memory, but it works */
	    String[] split = cmds.split(" ");
	    String[] revsplit = strcheck(split);
	    String[] finalarr = new String[revsplit.length + 1];
	    finalarr[0] = str;
	    for (int i = 0; i < revsplit.length; i++) {
		if (revsplit[i].equals("loc")) {		    
		    revsplit[i] = str;
		}
		finalarr[i + 1] = revsplit[i];
	    }
	    /* 
	       We don't need to send anything back because
	       the variable list is global and anything new
	       will remain in the runtime
	    */
	    //System.out.println(eval(finalarr));
	}
    }

    /* Input Mangling */
    private static String[] strcheck(String[] input) {
	/* 
	   Check for strings 
	   Because, like shell, the parsing is based on spaces, we need
	   a way to have strings that contain spaces. This is how I did it,
	   though there's probably a better way 
	*/
	String[] ret = new String[input.length];
	int i = 0;
	int strs = 0;
	String tmp = "";
	for (String str : input) {
	    /* Newline finagling */
	    str = str.replace("\\n", "\n");
	    if (str.startsWith("'") && (strs % 2 == 0)) {
		strs++;
		/* Remove first quote */
		tmp = str.replaceFirst("'", "");
		if (str.replaceFirst("'", "").endsWith("'")) {	    
		    strs++;
		    /* Chop off last quote */
		    ret[i] = str.split("'")[1];
		    i++;
		}
	    } else if (str.contains("'") && (strs % 2 != 0)) {
		strs++;
		tmp += " " + str.split("'")[0];
		ret[i] = tmp;
		i++;
	    } else if (strs % 2 != 0) {
		tmp += " " + str;
	    } else {
		ret[i] = str;
		i++;
	    }
	}
	return ret;
    }

    /* Entry point */
    public static void main(String[] args) {
	/* Check if we've got files */
	if (args.length > 0) {
	    for (String s : args) {
		Path path = Paths.get(s);
		try {
		    BufferedReader buf = Files.newBufferedReader(path);
		    String line;
		    while ((line = buf.readLine()) != null) {
			/* If the line starts with #, it's a comment */
			if (line.startsWith("#")) {
			    continue;
			} else {
			    String[] cmds = line.split(" ");
			    String[] revcmds = strcheck(cmds);
			    eval(revcmds);
			}
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		}
	    }
	} else {
	    /* Indicate status */
	    System.out.print(">> ");
	    /* Read */
	    /* The scanner auto-delimits on spaces, not newlines */
	    Scanner scan = new Scanner(System.in).useDelimiter("\n");
	    /* REPL */
	    while (true) {
		String input = scan.next();
		/* Split on spaces */
		String[] cmds = input.split(" ");
		String[] revcmds = strcheck(cmds);
		/* Eval */
		System.out.println("\n=> " + eval(revcmds));
		System.out.print(">> ");
	    }
	}
    }

    /* Object Zone */
    private static class mvariable {	
	/* Class to hold variables, because rolling my own associative array
	 * is a good use of my time
	 I *could* make this just have two lists, one with keys and one with 
	 values...
	 What's better: an object with two lists, or a list with a bunch of
	 objects?
	 */
	private String name;
	private String value;

	public mvariable(String initname, String initval) {
		name = initname;
		value = initval;
	}

	public String name() {
		return name;
	}

	public String value() {
		return value;
	}

	public void update(String val) {
	    value = val;
	}

	/* Overloading? In MY interpreter? More likely than you'd think */
	public void update(String val, int index) {
	    String[] arr = value.split(",");
	    arr[index] = val;
	    value = "";
	    for (String s : arr) {
		value += s + ",";
	    }
	    
	}
    }

    private static class mfunc {
	/*
	  Class for own-rolled functions/subroutines/whatever
	*/
	private String[] body;
	private String name;
	private String[] paramlist;
	private int params;
	public mfunc(String[] nbody, int nparams, String nname, String[] list) {
	    body = nbody;
	    name = nname;
	    params = nparams;
	    paramlist = list;
	}
	
	/* Accessors */
	public String name() {
	    return name;
	}
	
	public String[] body() {
	    return body;
	}
	
	public int params() {
	    return params;
	}

	public String[] call(String[] passedparams) {
	    /* 
	       Return an array of strings with the parameter vars replaced
	       with the real parameters passed
	    */
	    for (int i = 0; i < paramlist.length; i++) {
		for (int k = 0; k < body.length; k++) {
		    if (body[k].equals(paramlist[i])) {
			body[k] = passedparams[i];
		    }
		}
	    }
	    return body;
	}
    }
}
