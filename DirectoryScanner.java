import java.net.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.*;

public class DirectoryScanner{
	public static final int DELETE_FILE = 0;
	public static final int CREATE_FILE = 1;
	File path;
	Set<String> ori;

	public DirectoryScanner(File path){
		this.path = path;
		ori = getFiles(path);
	}

	public ChangeList update(){
		Set<String> nvo = getFiles(path);
		int size = Math.abs(nvo.size()-ori.size());
		List<String> names = new ArrayList<String>();
		List<Integer> vals = new ArrayList<Integer>();

		for(String name: nvo){
			if(!ori.contains(name)){
				names.add(name);
				vals.add(CREATE_FILE);
			}
		}

		for(String name: ori){
			if(!nvo.contains(name)){
				names.add(name);
				vals.add(DELETE_FILE);
			}
		}

		ChangeList ans = new ChangeList(names,vals);
		ori = nvo;
		return ans;
	}


	public static class ChangeList{
		public String[] names;
		public Integer[] vals;

		public ChangeList(List<String> names, List<Integer> vals){
			this.names = new String[names.size()];
			this.vals = new Integer[vals.size()];
			this.names = names.toArray(this.names);
			this.vals = vals.toArray(this.vals);
		}

	}

	public static Set<String> getFiles(File path){
		return new HashSet<String>(Arrays.asList(path.list()));
	}


	public static void main(String[] args){
		File path = new File(args[0]);
		DirectoryScanner ds = new DirectoryScanner(path);
		Scanner sc = new Scanner(System.in);
		int x = sc.nextInt();
		DirectoryScanner.ChangeList cl = ds.update();
		for(int i=0;i<cl.names.length;i++){
			System.out.println(cl.names[i] + " " + cl.vals[i] );
		}
	}


}