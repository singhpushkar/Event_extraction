package query_handle;

public class Test {
	public static void main(String[] args) {
		String x="utkarsh";
		int min =99999999,sum;
		for(int i=97;i<97+26;i++) {
			sum =0;
			for(int j=0;j<x.length();j++) {
					sum += Math.abs(x.charAt(j)-(char)i);
			}
			if(sum<min) {
				min=sum;
			}
		}
		System.out.println(min);
	}
}
