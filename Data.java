import java.io.*;
import java.lang.Object;
import java.awt.Color;
import java.awt.Font;
import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import org.apache.poi.poifs.filesystem.POIFSFileSystem; 
import org.apache.poi.*;
import org.apache.poi.ooxml.POIXMLDocumentPart;
import org.apache.poi.ooxml.POIXMLDocument;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.ss.usermodel.Cell;
import java.util.Iterator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
//import java.lang.Enum<CellType>;
//import org.apache.poi.ss.usermodel;


/**
 * Holds the information of a data set. Each row contains a single data point. Primary computations
 * of PCA are performed by the Data object.
 * @author	Kushal Ranjan
 * @version	051313
 */
class Data{
	double[][] matrix; 
	static double[][] data;//matrix[i] is the ith row; matrix[i][j] is the ith row, jth column
	static XSSFRow row;
	/**
	 * Constructs a new data matrix.
	 * @param vals	data for new Data object; dimensions as columns, data points as rows.
	 */
	Data(double[][] vals) {
		matrix = Matrix.copy(vals);

	}
	
	/**
	 * Test code. Constructs an arbitrary data table of 5 data points with 3 variables, normalizes
	 * it, and computes the covariance matrix and its eigenvalues and orthonormal eigenvectors.
	 * Then determines the two principal components.
	 */
	 		
	public static void main(String[] args) {
	
		 //final AtomicReference<Double[][]> data = new AtomicReference<Double[][]>(null);
		 
		 JFrame f= new JFrame();
		 Font f1 = new Font("TimesRoman",Font.BOLD,35);
		 Font f2 = new Font("TimesRoman",Font.BOLD,20);
		 JButton b=new JButton("load dataset");
		 JLabel l1,l2,l3;
		 JTextField t1,t2;
		 l1=new JLabel("Addressing the curse of dimensionality using PCA");
         l1.setForeground(Color.blue);
         l1.setFont(f1);
         l1.setBounds(300,20,850,150);
         l2=new JLabel("Enter file name :");
		 l2.setFont(f2);
         l2.setBounds(500,200,850,150);
         l3=new JLabel("Enter number of principal components do you want :");
         l3.setFont(f2);
         l3.setBounds(500,300,850,150);
         t1=new JTextField(" ");
		 JTextArea je=new JTextArea();
         t1.setBounds(660,260,150,30);
         t2=new JTextField();
		 String p=t2.getText();
         t2.setBounds(950,360,150,30);
         b.setBounds(600,500,150,30);
         f.add(l1);
         f.add(l2);
         f.add(l3);
         f.add(t1);
         f.add(t2);
         f.add(b);
         f.setSize(1280,800);
         f.setBackground(Color.red);
         f.setLayout(null);
         f.setVisible(true);
         f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		//double[][] data = parseData(args[0]);
		 b.addActionListener(new ActionListener() {
	   public void actionPerformed(ActionEvent ae) {
			String action = ae.getActionCommand();
			String file=t1.getText();
			String pcs=t2.getText();

			//int pca=Integer.parseInt(pcs);
			data=null;
			try {
			 data = parseData(file);
		} catch(ArrayIndexOutOfBoundsException e) {
			e.printStackTrace();
			System.err.println("Malformed data table.");
		} catch(IOException e) {
			System.err.println("Malformed data file.");
		}
		    
		 System.out.println("Raw data:");
		//1.DISPLAY DATA SET - RAW DATA    5X3  MATRIX FORM
		//Matrix.print(data);
		
		//2.STORE DATA IN 2D  VARIABLE OF AN OBJECT
		Data dat = new Data(data);
		
		//3.DATA NORMALIZATION -  MEAN CALCULATION
		dat.center();
				
		//4.FIND OUT COVARIANCE MATRIX
		double[][] cov = dat.covarianceMatrix();
		System.out.println("Covariance matrix:");
		Matrix.print(cov);
		
		//5.CALCULATE EIGEN VALUES
		EigenSet eigen = dat.getCovarianceEigenSet();
		double[][] vals = {eigen.values};
		System.out.println("Eigenvalues:");
		Matrix.print(vals);
		
		//6.CALCULATE EIGEN VECTORS
		System.out.println("Corresponding eigenvectors:");
		Matrix.print(eigen.vectors);
		System.out.println("Two principal components:");
				try{
				
				Matrix.print(dat.buildPrincipalComponents(Integer.parseInt(pcs), eigen));
				System.out.println("Principal component transformation:");
				Matrix.print(Data.principalComponentAnalysis(data, Integer.parseInt(pcs)));
				}catch(NumberFormatException ex){ // handle your exception
						
					}
		
	   }
		 });
		 
	   
	}
	
	/**
	 * PCA implemented using the NIPALS algorithm. The return value is a double[][], where each
	 * double[] j is an array of the scores of the jth data point corresponding to the desired
	 * number of principal components.
	 * @param input			input raw data array
	 * @param numComponents	desired number of PCs
	 * @return				the scores of the data array against the PCS
	 */
	static double[][] PCANIPALS(double[][] input, int numComponents) {
		Data data = new Data(input);
		data.center();
		double[][][] PCA = data.NIPALSAlg(numComponents);
		double[][] scores = new double[numComponents][input[0].length];
		for(int point = 0; point < scores[0].length; point++) {
			for(int comp = 0; comp < PCA.length; comp++) {
				scores[comp][point] = PCA[comp][0][point];
			}
		}
		return scores;
	}
	
	/**
	 * Implementation of the non-linear iterative partial least squares algorithm on the data
	 * matrix for this Data object. The number of PCs returned is specified by the user.
	 * @param numComponents	number of principal components desired
	 * @return				a double[][][] where the ith double[][] contains ti and pi, the scores
	 * 						and loadings, respectively, of the ith principal component.
	 */
	double[][][] NIPALSAlg(int numComponents) {
		final double THRESHOLD = 0.00001;
		double[][][] out = new double[numComponents][][];
		double[][] E = Matrix.copy(matrix);
		for(int i = 0; i < out.length; i++) {
			double eigenOld = 0;
			double eigenNew = 0;
			double[] p = new double[matrix[0].length];
			double[] t = new double[matrix[0].length];
			double[][] tMatrix = {t};
			double[][] pMatrix = {p};
			for(int j = 0; j < t.length; j++) {
				t[j] = matrix[i][j];
			}
			do {
				eigenOld = eigenNew;
				double tMult = 1/Matrix.dot(t, t);
				tMatrix[0] = t;
				p = Matrix.scale(Matrix.multiply(Matrix.transpose(E), tMatrix), tMult)[0];
				p = Matrix.normalize(p);
				double pMult = 1/Matrix.dot(p, p);
				pMatrix[0] = p;
				t = Matrix.scale(Matrix.multiply(E, pMatrix), pMult)[0];
				eigenNew = Matrix.dot(t, t);
			} while(Math.abs(eigenOld - eigenNew) > THRESHOLD);
			tMatrix[0] = t;
			pMatrix[0] = p;
			double[][] PC = {t, p}; //{scores, loadings}
			E = Matrix.subtract(E, Matrix.multiply(tMatrix, Matrix.transpose(pMatrix)));
			out[i] = PC;
		}
		return out;
	}
	
	/**
	 * Previous algorithms for performing PCA
	 */
	
	/**
	 * Performs principal component analysis with a specified number of principal components.
	 * @param input			input data; each double[] in input is an array of values of a single
	 * 						variable for each data point
	 * @param numComponents	number of components desired
	 * @return				the transformed data set
	 */
	static double[][] principalComponentAnalysis(double[][] input, int numComponents) {
		Data data = new Data(input);
		data.center();
		EigenSet eigen = data.getCovarianceEigenSet();
		double[][] featureVector = data.buildPrincipalComponents(numComponents, eigen);
		double[][] PC = Matrix.transpose(featureVector);
		double[][] inputTranspose = Matrix.transpose(input);
		return Matrix.transpose(Matrix.multiply(PC, inputTranspose));
	}
	
	/**
	 * Returns a list containing the principal components of this data set with the number of
	 * loadings specified.
	 * @param numComponents	the number of principal components desired
	 * @param eigen			EigenSet containing the eigenvalues and eigenvectors
	 * @return				the numComponents most significant eigenvectors
	 */
	double[][] buildPrincipalComponents(int numComponents, EigenSet eigen) {
		double[] vals = eigen.values;
		if(numComponents > vals.length) {
			throw new RuntimeException("Cannot produce more principal components than those provided.");
		}
		boolean[] chosen = new boolean[vals.length];
		double[][] vecs = eigen.vectors;
		double[][] PC = new double[numComponents][];
		for(int i = 0; i < PC.length; i++) {
			int max = 0;
			while(chosen[max]) {
				max++;
			}
			for(int j = 0; j < vals.length; j++) {
				if(Math.abs(vals[j]) > Math.abs(vals[max]) && !chosen[j]) {
					max = j;
				}
			}
			chosen[max] = true;
			PC[i] = vecs[max];
		}
		return PC;
	}
	
	/**
	 * Uses the QR algorithm to determine the eigenvalues and eigenvectors of the covariance 
	 * matrix for this data set. Iteration continues until no eigenvalue changes by more than 
	 * 1/10000.
	 * @return	an EigenSet containing the eigenvalues and eigenvectors of the covariance matrix
	 */
	EigenSet getCovarianceEigenSet() {
		
		double[][] data = covarianceMatrix();
		
		return Matrix.eigenDecomposition(data);
	}
	
	/**
	 * Constructs the covariance matrix for this data set.
	 * @return	the covariance matrix of this data set
	 */
	double[][] covarianceMatrix() {
		//THE SIZE OF COVARIANCE MATRIX SIZE=3X3  (BCOZ WE ARE USING 3 VARIABLES/COLUMNS)
		double[][] out = new double[matrix.length][matrix.length];
		//System.out.println("==============LENGTH================"+out.length);
		for(int i = 0; i < out.length; i++) {
			for(int j = 0; j < out.length; j++) {
				double[] dataA = matrix[i];
				double[] dataB = matrix[j];
				out[i][j] = covariance(dataA, dataB);  //CALCULATE COVARIANCE
			}
		}
		return out;
	}
	
	/**
	 * Returns the covariance of two data vectors.
	 * @param a	double[] of data
	 * @param b	double[] of data
	 * @return	the covariance of a and b, cov(a,b)
	 */
	static double covariance(double[] a, double[] b) {
		if(a.length != b.length) {
			throw new MatrixException("Cannot take covariance of different dimension vectors.");
		}
		double divisor = a.length - 1;               //NUMBER OF ROWS - 1
		double sum = 0;
		double aMean = mean(a);
		double bMean = mean(b);
		for(int i = 0; i < a.length; i++) {
			sum += (a[i] - aMean) * (b[i] - bMean);
		}
		return sum/divisor;
	}
	
	/**
	 * Centers each column of the data matrix at its mean.
	 */
	void center() {
		matrix = normalize(matrix);
	}
	
	
	/**
	 * Normalizes the input matrix so that each column is centered at 0.
	 */
	double[][] normalize(double[][] input) {
		
		double[][] out = new double[input.length][input[0].length];      //  5  - COLS IN EACH ROW       ROWS=3
		
		for(int i = 0; i < input.length; i++) {
			double mean = mean(input[i]);
			for(int j = 0; j < input[i].length; j++) {
				out[i][j] = input[i][j] - mean;
				////
				//System.out.println(out[i][j]);
				//System.out.print(" ");
			}//for
			//System.out.println("\n");
		}//for
		return out;
	}
	
	/**
	 * Calculates the mean of an array of doubles.
	 * @param entries	input array of doubles
	 */
	static double mean(double[] entries) {
		double out = 0;
		
		for(double d: entries) {
		 out += d/entries.length;
	     }
		 //System.out.println("The Mean is :"+out);
		 //System.out.println("The Normalized data is....");
		return out;
	}
	static double[][] parseData(String filename) throws IOException,NullPointerException{
		BufferedReader in = null;
		int i=0;
		int j=0;
		try {
			in = new BufferedReader(new FileReader(new File(filename)));
		} catch(FileNotFoundException e) {
			System.err.println("File " + filename + " not found.");
		}
		//String firstLine = in.readLine();
		XSSFWorkbook workbook = new XSSFWorkbook(filename);
 
            //Get first/desired sheet from the workbook
            XSSFSheet sheet = workbook.getSheetAt(0);
            DataFormatter dataFormatter = new DataFormatter();
 
            //Iterate through each rows one by one
            Iterator<Row> rowIterator = sheet.iterator();
            while (rowIterator.hasNext())
            {
                Row row = rowIterator.next();
                //For each row, iterate through all the columns
                Iterator<Cell> cellIterator = row.cellIterator();
                 
                while (cellIterator.hasNext())
                {
                    Cell cell = cellIterator.next();
                    //Check the cell type and format accordingly
                   // switch (cell.getCellType())
                   // {
                    	String cellValue = dataFormatter.formatCellValue(cell);
                            data[i][j] = Double.parseDouble(cellValue);
                            break;
                       // case STRING:
                        //    System.out.print(cell.getStringCellValue() + "t");
                          //  break;
                  //  }
                }
                //System.out.println("");
            }
           // in.close();
        //}
       // catch (Exception e)
       // {
        //    e.printStackTrace();
        //}
		//String[] dims = firstLine.split("	"); // <# points> <#dimensions>
		//double[][] data = new double[Integer.parseInt(dims[1])][Integer.parseInt(dims[0])];
		//for(int j = 0; j < data[0].length; j++) {
			//String text = in.readLine();
			//String[] vals = text.split("	");
			//for(int i = 0; i < data.length; i++)  {
				//data[i][j] = Double.parseDouble(vals[i]);
				
			//}
		//}
		try {
			in.close();
		} catch(IOException e) {
			e.printStackTrace();
		}
		return data;
	}
	static void print(double[][] matrix) {
		for(int j = 0; j < matrix[0].length; j++) {
			for(int i = 0; i < matrix.length; i++) {
				double formattedValue = Double.parseDouble(String.format("%.4g%n", matrix[i][j]));
				if(Math.abs(formattedValue) < 0.00001) { //Hide negligible values
					formattedValue = 0;
				}
				System.out.print(formattedValue + "\t");
			}
			System.out.print("\n");
		}
		System.out.println("");
	}
	
}
	
