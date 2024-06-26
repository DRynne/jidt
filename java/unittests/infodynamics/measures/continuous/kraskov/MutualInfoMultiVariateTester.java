/*
 *  Java Information Dynamics Toolkit (JIDT)
 *  Copyright (C) 2012, Joseph T. Lizier
 *  
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *  
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *  
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package infodynamics.measures.continuous.kraskov;

import infodynamics.utils.ArrayFileReader;
import infodynamics.utils.MathsUtils;
import infodynamics.utils.MatrixUtils;
import infodynamics.utils.RandomGenerator;

public class MutualInfoMultiVariateTester
	extends infodynamics.measures.continuous.MutualInfoMultiVariateAbstractTester {

	protected String NUM_THREADS_TO_USE_DEFAULT = MutualInfoCalculatorMultiVariateKraskov.USE_ALL_THREADS;
	protected String NUM_THREADS_TO_USE = NUM_THREADS_TO_USE_DEFAULT;
	
	/**
	 * Utility function to create a calculator for the given algorithm number
	 * 
	 * @param algNumber
	 * @return
	 */
	public MutualInfoCalculatorMultiVariateKraskov getNewCalc(int algNumber) {
		MutualInfoCalculatorMultiVariateKraskov miCalc = null;
		if (algNumber == 1) {
			miCalc = new MutualInfoCalculatorMultiVariateKraskov1();
		} else if (algNumber == 2) {
			miCalc = new MutualInfoCalculatorMultiVariateKraskov2();
		}
		return miCalc;
	}
	
	/**
	 * Confirm that the local values average correctly back to the average value
	 * 
	 * 
	 */
	public void checkLocalsAverageCorrectly(int algNumber, String numThreads) throws Exception {
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(algNumber);
		
		String kraskov_K = "4";
		
		miCalc.setProperty(
				MutualInfoCalculatorMultiVariateKraskov.PROP_K,
				kraskov_K);
		miCalc.setProperty(
				MutualInfoCalculatorMultiVariateKraskov.PROP_NUM_THREADS,
				numThreads);

		super.testLocalsAverageCorrectly(miCalc, 2, 10000);
	}
	public void testLocalsAverageCorrectly() throws Exception {
		checkLocalsAverageCorrectly(1, NUM_THREADS_TO_USE);
		checkLocalsAverageCorrectly(2, NUM_THREADS_TO_USE);
	}
	
	/**
	 * Confirm that significance testing doesn't alter the average that
	 * would be returned.
	 * 
	 * @throws Exception
	 */
	public void checkComputeSignificanceDoesntAlterAverage(int algNumber) throws Exception {
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(algNumber);
		
		String kraskov_K = "4";
		
		miCalc.setProperty(
				MutualInfoCalculatorMultiVariateKraskov.PROP_K,
				kraskov_K);
		miCalc.setProperty(
				MutualInfoCalculatorMultiVariateKraskov.PROP_NUM_THREADS,
				NUM_THREADS_TO_USE);

		super.testComputeSignificanceDoesntAlterAverage(miCalc, 2, 100);
	}
	public void testComputeSignificanceDoesntAlterAverage() throws Exception {
		checkComputeSignificanceDoesntAlterAverage(1);
		checkComputeSignificanceDoesntAlterAverage(2);
	}
	
	/**
	 * Utility function to run Kraskov MI for data with known results.
	 * Sets to use no noise in the calculation and a tolerance of 0.0000001
	 * 
	 * @param var1
	 * @param var2
	 * @param kNNs array of Kraskov k nearest neighbours parameter to check
	 * @param expectedResults array of expected results for each k
	 * @return errors of the computed values against expectedResults
	 */
	protected double[] checkMIForGivenData(double[][] var1, double[][] var2,
			int[] kNNs, double[] expectedResults) throws Exception {
		// Dropping required accuracy by one order of magnitude, due
		//  to faster but slightly less accurate digamma estimator change
		return checkMIForGivenData(var1, var2, kNNs, expectedResults, 0, "NONE", 0.0000001);
	}
	
	/**
	 * Utility function to run Kraskov MI for data with known results
	 * 
	 * @param var1
	 * @param var2
	 * @param kNNs array of Kraskov k nearest neighbours parameter to check
	 * @param expectedResults array of expected results for each k
	 * @param noiseLevel noise to add to the data - set to 0 if we 
	 *   need to exactly reproduce calculations (most cases in unit tests for consistency)
	 * @param noiseSeed seed for the random noise generator (either "NONE" or Long string)
	 * @param tolerance tolerance to accept the calculation
	 * @return errors of the computed values against expectedResults
	 */
	protected double[] checkMIForGivenData(double[][] var1, double[][] var2,
			int[] kNNs, double[] expectedResults,
			double noiseLevel, String noiseSeed, double tolerance) throws Exception {
			
		// The Kraskov MILCA toolkit MIhigherdim executable 
		//  uses algorithm 2 by default (this is what it means by rectangular):
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(2);
		double[] errors = new double[expectedResults.length]; 
		
		for (int kIndex = 0; kIndex < kNNs.length; kIndex++) {
			int k = kNNs[kIndex];
			miCalc.setProperty(
					MutualInfoCalculatorMultiVariateKraskov.PROP_K,
					Integer.toString(k));
			miCalc.setProperty(
					MutualInfoCalculatorMultiVariateKraskov.PROP_NUM_THREADS,
					NUM_THREADS_TO_USE);
			// No longer need to set this property as it's set by default:
			//miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_NORM_TYPE,
			//		EuclideanUtils.NORM_MAX_NORM_STRING);
			miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_ADD_NOISE,
					Double.toString(noiseLevel));
			miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_NOISE_SEED, noiseSeed);
			miCalc.initialise(var1[0].length, var2[0].length);
			miCalc.setObservations(var1, var2);
			miCalc.setDebug(true);
			double mi = miCalc.computeAverageLocalOfObservations();
			miCalc.setDebug(false);
			
			System.out.printf("k=%d: Average MI %.8f (expected %.8f)\n",
					k, mi, expectedResults[kIndex]);
			assertEquals(expectedResults[kIndex], mi, tolerance);
			errors[kIndex] = mi - expectedResults[kIndex];
		}
		return errors;
	}
	
	/**
	 * Test the computed univariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 1 1 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testUnivariateMIforRandomVariablesFromFile() throws Exception {
		
		// Test set 1:
		
		ArrayFileReader afr = new ArrayFileReader("demos/data/2randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA = {-0.05294175, -0.03944338, -0.02190217,
				0.00120807, -0.00924771, -0.00316402, -0.00778205, -0.00565778};
		
		System.out.println("Kraskov comparison 1 - univariate random data 1");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, expectedFromMILCA);
		
		//------------------
		// Test set 2:
		
		// We'll just take the first two columns from this data set
		afr = new ArrayFileReader("demos/data/4randomCols-1.txt");
		data = afr.getDouble2DMatrix();
		
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {-0.04614525, -0.00861460, -0.00164540,
				-0.01130354, -0.01339670, -0.00964035, -0.00237072, -0.00096891};
		
		System.out.println("Kraskov comparison 2 - univariate random data 2");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 2 2 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIforRandomVariablesFromFile() throws Exception {
		
		// Test set 3:
		
		// We'll just take the first two columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/4randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.02886644, 0.01071634, 0.00186857,
				-0.00377259, -0.00634851, -0.00863725, -0.01058087, -0.01106348};
		
		System.out.println("Kraskov comparison 3 - multivariate random data 1");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1}),
				MatrixUtils.selectColumns(data, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data, using various numbers of threads
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 2 2 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIVariousNumThreads() throws Exception {
		
		// Test set 3a and 3b:
		
		// We'll just take the first two columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/4randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {3, 4};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.00186857,
				-0.00377259};
		
		System.out.println("Kraskov comparison 3a - single threaded");
		NUM_THREADS_TO_USE = "1";
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1}),
				MatrixUtils.selectColumns(data, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);
		System.out.println("Kraskov comparison 3b - dual threaded");
		NUM_THREADS_TO_USE = "2";
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1}),
				MatrixUtils.selectColumns(data, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);
		NUM_THREADS_TO_USE = NUM_THREADS_TO_USE_DEFAULT;

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 1 3 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testImbalancedMultivariateMIforRandomVariablesFromFile() throws Exception {
		
		// Test set 4:
		
		// We'll take MI from first column to the next 3:
		ArrayFileReader afr = new ArrayFileReader("demos/data/4randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA = {0.02473475, 0.00404451, -0.00454679,
				-0.00737512, -0.00464896, -0.00610772, -0.00881741, -0.01306668};
		
		System.out.println("Kraskov comparison 4 - multivariate random data 2 (1 var to 3 vars)");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1, 2, 3}),
				kNNs, expectedFromMILCA);
	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * This also tests for multithreading with residuals, assuming
	 *  we're running on a 4 processor machine
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 2 2 3030 <kNearestNeighbours> 0
	 * where the file has the first 30 rows repeated.
	 * 
	 * Kraskov et al recommend that a small amount of noise should be 
	 * added to the data to avoid issues with repeated scores; this
	 * can be done in our toolkit by setting the relevant property
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIforRandomVariablesRepeatedDataFromFile() throws Exception {
		
		// Test set 5:
		
		// We'll just take the first two columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/4randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		double[][] data2 = new double[data.length + 30][data[0].length];
		for (int r = 0; r < data.length; r++) {
			for (int c = 0; c < data[r].length; c++) {
				data2[r][c] = data[r][c];
			}
		}
		// Repeat the first 30 rows:
		for (int r = 0; r < 30; r++) {
			for (int c = 0; c < data[r].length; c++) {
				data2[r+data.length][c] = data[r][c];
			}			
		}
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.16846374, 0.04091779, 0.02069109,
				0.00700680, 0.00121768, -0.00134164, -0.00870685, -0.00966508};
		
		System.out.println("Kraskov comparison 5 - multivariate random data 1 with 30 repeated rows");
		checkMIForGivenData(MatrixUtils.selectColumns(data2, new int[] {0, 1}),
				MatrixUtils.selectColumns(data2, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);
	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 2 2 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIforDependentVariablesFromFile() throws Exception {
		
		// Test set 6:
		
		// We'll just take the first two columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/4ColsPairedDirectDependence-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {5.00322122, 4.29011291, 3.91312749, 
				3.69192886, 3.52807488, 3.39865354, 3.05327646, 2.79951639};
		
		System.out.println("Kraskov comparison 6 - multivariate dependent data 1");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1}),
				MatrixUtils.selectColumns(data, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 2 2 3000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIforNoisyDependentVariablesFromFile() throws Exception {
		
		// Test set 7:
		
		// We'll just take the first two columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/4ColsPairedNoisyDependence-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.33738970, 0.36251531, 0.34708687, 
				0.36200563, 0.35766125, 0.35007623, 0.35023664, 0.33728287};
		
		System.out.println("Kraskov comparison 7 - multivariate dependent data 1");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1}),
				MatrixUtils.selectColumns(data, new int[] {2, 3}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 5 5 10000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMultivariateMIforRandomGaussianVariablesFromFile() throws Exception {
		
		// Test set 8:
		
		// We'll take the columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/10ColsRandomGaussian-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 4, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.00815609, 0.00250864, 0.00035825,
				0.00172174, 0.00033354};
		
		System.out.println("Kraskov comparison 8 - multivariate uncorrelated Gaussian data 1");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0, 1, 2, 3, 4}),
				MatrixUtils.selectColumns(data, new int[] {5, 6, 7, 8, 9}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Test the computed multivariate MI against that calculated by Kraskov's own MILCA
	 * tool on the same data.
	 * 
	 * To run Kraskov's tool (http://www.klab.caltech.edu/~kraskov/MILCA/) for this 
	 * data, run:
	 * ./MIxnyn <dataFile> 1 1 10000 <kNearestNeighbours> 0
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testMIforRandomGaussianVariablesFromLargeFile() throws Exception {
		
		// Test set 9:
		
		// We'll take the columns from this data set
		ArrayFileReader afr = new ArrayFileReader("demos/data/10ColsRandomGaussian-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 4, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA_2 = {0.01542004, 0.01137151, 0.00210945,
				0.00159921, 0.00031277};
		
		System.out.println("Kraskov comparison 9 - uncorrelated Gaussian data 1 - large file");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, expectedFromMILCA_2);

	}

	/**
	 * Unit test for MI on new observations.
	 * We can test this against the calculator itself. If we send in the original data set as new observations,
	 *  we can recreate the neighbour counts (plus one) by setting K to 1 larger (to account for the data point itself), and
	 *  account for the change in bias. 
	 * 
	 * @throws Exception
	 */
	public void testMultivariateCondMIForNewObservations() throws Exception {
		
		ArrayFileReader afr = new ArrayFileReader("demos/data/4ColsPairedOneStepNoisyDependence-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		// RandomGenerator rg = new RandomGenerator();
		//double[][] data = rg.generateNormalData(50, 4, 0, 1);

		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {4, 10, 15};
		
		System.out.println("Kraskov MI testing new Observations:");
		
		for (int alg = 1; alg < 3; alg++) {
			for (int ki = 0; ki < kNNs.length; ki++) {
				MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(alg);
				MutualInfoCalculatorMultiVariateKraskov miCalcForNew = getNewCalc(alg);
				// Let it normalise by default
				// And no noise addition to protect the integrity of our neighbour counts under both techniques here:
				miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_ADD_NOISE, "0");
				miCalcForNew.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_ADD_NOISE, "0");
				double[][] var1 = MatrixUtils.selectColumns(data, new int[] {0});
				double[][] var2 = MatrixUtils.selectColumns(data, new int[] {1});
				
				// Compute MI(0;1|2,3) :
				miCalc.setProperty(
						MutualInfoCalculatorMultiVariateKraskov.PROP_K,
						Integer.toString(kNNs[ki]));
				System.out.println("Main calc normalisation is " + miCalc.getProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_NORMALISE));
				miCalc.initialise(var1[0].length, var2[0].length);
				miCalc.setObservations(var1, var2);
				@SuppressWarnings("unused")
				double miAverage = miCalc.computeAverageLocalOfObservations();
				// Now compute as new observations:
				miCalcForNew.setProperty(
						MutualInfoCalculatorMultiVariateKraskov.PROP_K,
						Integer.toString(kNNs[ki] + 1)); // Using K = K + 1
				// condMiCalcForNew.setProperty(
				//		MutualInfoCalculatorMultiVariateKraskov.PROP_NUM_THREADS,
				//		"1");
				System.out.println("New obs calc normalisation is " + miCalcForNew.getProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_NORMALISE));
				miCalcForNew.initialise(var1[0].length, var2[0].length);
				miCalcForNew.setObservations(var1, var2);
				// condMiCalc.setDebug(true);
				//condMiCalcForNew.setDebug(true);
				double[] newLocals = miCalcForNew.computeLocalUsingPreviousObservations(var1, var2);
				//condMiCalcForNew.setDebug(false);
				@SuppressWarnings("unused")
				double averageFromNewObservations = MatrixUtils.mean(newLocals);
				// We can't check this directly, so test each point individually:
				for (int t = 0; t < data.length; t++) {
					double[] originalNeighbourCounts = miCalc.partialComputeFromObservations(t, 1, false);
					// Need to normalise the data before passing it in here -- this is
					//  what is happening inside computeLocalUsingPreviousObservations above
					double[] newObsNeighbourCounts = miCalcForNew.partialComputeFromNewObservations(
							t, 1,
							MatrixUtils.normaliseIntoNewArray(var1),
							MatrixUtils.normaliseIntoNewArray(var2), false);
					// Now check each return count in the array:
					if (originalNeighbourCounts[1] != newObsNeighbourCounts[1] - 1) {
						System.out.println("Assertion failure for t=" + t + ": expected " + originalNeighbourCounts[1] +
								" from original, plus 1, but got " + newObsNeighbourCounts[1]);
						System.out.print("Actual raw data was: ");
						MatrixUtils.printArray(System.out, data[0]);
					}
					assertEquals(originalNeighbourCounts[1], newObsNeighbourCounts[1] - 1); // Nx should be 1 higher
					assertEquals(originalNeighbourCounts[2], newObsNeighbourCounts[2] - 1); // Ny should be 1 higher
					// Now check the local value at each point using these verified counts:
					double newLocalValue;
					if (alg == 1) {
						newLocalValue = miCalcForNew.digammaK -
							MathsUtils.digamma((int) newObsNeighbourCounts[1] + 1) -
							MathsUtils.digamma((int) newObsNeighbourCounts[2] + 1) +
							MathsUtils.digamma(miCalcForNew.getNumObservations() + 1); // correct digammaN for new samples
					} else {
						newLocalValue = miCalcForNew.digammaK -
							(double) 1 / (double) miCalcForNew.k -
							MathsUtils.digamma((int) newObsNeighbourCounts[1]) -
							MathsUtils.digamma((int) newObsNeighbourCounts[2]) +
							MathsUtils.digamma(miCalcForNew.getNumObservations() + 1); // correct digammaN for new samples
					}
					if (Math.abs(newLocalValue - newLocals[t]) > 0.00000001) {
						System.out.printf("t=%d: Assertion failed: computed local was %.5f, local from nn counts was %.5f\n",
								t, newLocals[t], newLocalValue);
					}
					assertEquals(newLocalValue, newLocals[t], 0.00000001);
				}
			}
		}
	}

	/**
	 * Test the experimental conditional entropy method
	 *  on random Gaussians
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testConditionalEntropyforNoisyIndependentVariablesFromFile() throws Exception {
		
		// We'll just take the first two columns from this data set
		// Works well on 10ColsRandomGaussian-1.txt because it is Gaussian
		ArrayFileReader afr = new ArrayFileReader("demos/data/10ColsRandomGaussian-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// When normalising the marginals to std dev 1, we know that entropy of the marginal
		//  is expected to be:
		double expected_Hx = 0.5 * Math.log(2.0 * Math.PI * Math.E);
		
		System.out.println("Kraskov comparison - conditional entropy");
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(1);
		
		miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_ADD_NOISE, "0"); // Need consistency for unit tests
		
		int[] numSamplesToCheck = new int[] {1000, 3000, 5000, data.length};
		double conditionalEnt = 0, expected_H_X_given_Y = 0;
		for (int ni = 0; ni < numSamplesToCheck.length; ni++) {
			miCalc.initialise(1, 1);
			miCalc.setObservations(
					MatrixUtils.selectRowsAndColumns(data, 0, numSamplesToCheck[ni], 0, 1),
					MatrixUtils.selectRowsAndColumns(data, 0, numSamplesToCheck[ni], 1, 1));
			miCalc.setDebug(true);
			double mi = miCalc.computeAverageLocalOfObservations();
			miCalc.setDebug(false);
			conditionalEnt = miCalc.computeAverageConditionalEntropy();
			expected_H_X_given_Y = -mi + expected_Hx;
			
			System.out.printf("k=%s: Average MI %.8f; Average H(X|Y) = %.8f (expected %.8f) from %d samples\n",
					miCalc.getProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_K), mi,
					conditionalEnt, expected_H_X_given_Y, miCalc.getNumObservations());
		}
		// Only check the assertion on the full data set:
		assertEquals(expected_H_X_given_Y, conditionalEnt, 0.01);

	}

	/**
	 * Test the experimental conditional entropy method
	 *  on random coupled uniform variables
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testConditionalEntropyforNoisyDependentVariablesFromFile() throws Exception {
		
		// We'll just take the first two columns from this data set
		// We'll use 4ColsPairedNoisyDependence where first column is randomly distributed
		//  on 0..1 and third adds some noise to that.
		ArrayFileReader afr = new ArrayFileReader("demos/data/4ColsPairedNoisyDependence-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// When the marginal x is uniformly distributed on 0..1, we know that entropy of the marginal
		//  is expected to be 0 -- when we don't normalise the variables!
		double expected_Hx = 0;
		
		System.out.println("Kraskov comparison - conditional entropy");
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(1);
		
		miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_ADD_NOISE, "0"); // Need consistency for unit tests
		miCalc.setProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_NORMALISE, "false"); // Need to avoid normalising for the expected value to hold
		
		int[] numSamplesToCheck = new int[] {1000, 2000, data.length};
		double conditionalEnt = 0, expected_H_X_given_Y = 0;
		for (int ni = 0; ni < numSamplesToCheck.length; ni++) {
			miCalc.initialise(1, 1);
			miCalc.setObservations(
					MatrixUtils.selectRowsAndColumns(data, 0, numSamplesToCheck[ni], 0, 1),
					MatrixUtils.selectRowsAndColumns(data, 0, numSamplesToCheck[ni], 2, 1));
			double mi = miCalc.computeAverageLocalOfObservations();
			conditionalEnt = miCalc.computeAverageConditionalEntropy();
			expected_H_X_given_Y = -mi + expected_Hx;
			
			System.out.printf("k=%s: Average MI %.8f; Average H(X|Y) = %.8f (expected %.8f) from %d samples\n",
					miCalc.getProperty(MutualInfoCalculatorMultiVariateKraskov.PROP_K), mi,
					conditionalEnt, expected_H_X_given_Y, miCalc.getNumObservations());
		}
		// Only check the assertion on the full data set:
		assertEquals(expected_H_X_given_Y, conditionalEnt, 0.02);

	}
	
	/**
	 * Test that observationSetIndices and observationStartTimePoints are written properly
	 * 
	 * @throws Exception 
	 */
	public void testObservationSetIndices() throws Exception {
		
		int dimensions = 1;
		int timeSteps = 100;
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(1);
		miCalc.initialise(dimensions, dimensions);
		
		// generate some random data
		RandomGenerator rg = new RandomGenerator();
		double[][] sourceData = rg.generateNormalData(timeSteps, dimensions,
				0, 1);
		double[][] destData = rg.generateNormalData(timeSteps, dimensions,
				0, 1);
		
		// First check that for a simple single observation set everything works:
		miCalc.setObservations(sourceData, destData);

		int[] observationSetIds = miCalc.getObservationSetIndices();
		int[] timeSeriesIndices = miCalc.getObservationTimePoints();
		assert(observationSetIds.length == timeSteps);
		for (int t = 0; t < timeSteps; t++) {
			assertEquals(observationSetIds[t], 0);
			assertEquals(timeSeriesIndices[t], t);
		}
		
		// Now add the same one twice:
		miCalc.initialise(dimensions, dimensions);
		miCalc.startAddObservations();
		miCalc.addObservations(sourceData, destData);
		miCalc.addObservations(sourceData, destData);
		miCalc.finaliseAddObservations();
		observationSetIds = miCalc.getObservationSetIndices();
		timeSeriesIndices = miCalc.getObservationTimePoints();
		assert(observationSetIds.length == 2*timeSteps);
		for (int t = 0; t < timeSteps; t++) {
			assertEquals(observationSetIds[t], 0);
			assertEquals(timeSeriesIndices[t], t);
		}
		for (int t = 0; t < timeSteps; t++) {
			assertEquals(observationSetIds[timeSteps + t], 1);
			assertEquals(timeSeriesIndices[timeSteps + t], t);
		}
		
		// Now add NUM_SEGMENTS randomly chosen segments:
		int NUM_SEGMENTS = 10;
		int maxLength = 10;
		int[] startPoints = rg.generateRandomInts(NUM_SEGMENTS, timeSteps - maxLength);
		int[] lengthsMinus1 = rg.generateRandomInts(NUM_SEGMENTS, maxLength - 1); // ensures we don't add segments of length 0
		miCalc.initialise(dimensions, dimensions);
		miCalc.startAddObservations();
		for (int r = 0; r < NUM_SEGMENTS; r++) {
			miCalc.addObservations(sourceData, destData, startPoints[r], lengthsMinus1[r]+1);
		}
		miCalc.finaliseAddObservations();
		observationSetIds = miCalc.getObservationSetIndices();
		timeSeriesIndices = miCalc.getObservationTimePoints();
		assert(observationSetIds.length == MatrixUtils.sum(lengthsMinus1) + NUM_SEGMENTS);
		int t = 0;
		for (int r = 0; r < NUM_SEGMENTS; r++) {
			for (int i = 0; i < lengthsMinus1[r]+1; i++) {
				assertEquals(observationSetIds[t], r);
				assertEquals(timeSeriesIndices[t], startPoints[r] + i);
				t++;
			}
		}
	}

	/**
	 * Test the MI values returned, and that we can basically turn dyn corr excl
	 *  off by adding each data point in separately
	 * @throws Exception 
	 */
	public void testDynCorrExclAndSetIndices2() throws Exception {
		int nForEach = 100;
		double[] x = new double[nForEach * 4];
		double[] y = new double[nForEach * 4];
		
		int k = 4; // Make sure this is even
		int exclWindow = 2; // Make sure this is capped by k/2. Leave this at 2 - these of the code assumes this
		
		int segment = 0;
		for (int rx = 0; rx < 2; rx++) {
			for (int ry = 0; ry < 2; ry++) {
				for (int t = 0; t < nForEach; t++) {
					// x and y coordinate will be == t % nForEach when rx==0 or ry==0,
					//  else we will push them up by nForEach*2..
					// So we will basically have diagonal lines in four quadrants.
					x[segment*nForEach + t] = t + ((rx == 1) ? nForEach*2 : 0);
					y[segment*nForEach + t] = t + ((ry == 1) ? nForEach*2 : 0);
				}
				segment++;
			}
		}
		// We're going to set kNNs to be even, so that the nearest neighbours must be 
		//  the kNN closest points on the same line segement. n_x and n_y will be 2*(kNN) + 1 respectively with algorithm 2,
		//  since they will include the kNN in the same line segment (>= than the kNNth), plus the 
		//  corresponding point and it's kNNs in the segment with the same x or y range.
		// So we have:
		double expectedMIWithoutDynCorrExcl = MathsUtils.digamma(k) - 1.0 / (double)k - 2*MathsUtils.digamma(2*k+1) + MathsUtils.digamma(nForEach*4);
		// But if we turn on dynamic correlation exclusion, say for 2 points, then the kNNs will now step outside the previous k
		//  and the kernel widths for n_x and n_y will be wider by approx 2x the exclusion window size.
		// For points not on the edges, n_x and n_y will increase by 2x the exclusion window size up to k/2 since they
		//  will include this many more points in the corresponding segments.
		// For points near the edges, n_x and n_y will increase only by 1x the exclusion window on one side (where it doesn't abut the edge), and the amount of the 
		//  exclusion window that includes points on the other side.
		// The maths we're using here just assumes that the window is size 2 to make coding it fast (we're not going to bother changing it)
		double expectedMIWithDynCorrExcl =
				(double) ((nForEach-4) * 4) / (double) (nForEach * 4) * (MathsUtils.digamma(k) - 1.0 / (double)k - 2*MathsUtils.digamma(2*k+1+2*exclWindow) + MathsUtils.digamma(nForEach*4)) +
				(double) ((2) * 4) / (double) (nForEach * 4) * (MathsUtils.digamma(k) - 1.0 / (double)k - 2*MathsUtils.digamma(2*k+1+exclWindow + exclWindow-1) + MathsUtils.digamma(nForEach*4)) +
				(double) ((2) * 4) / (double) (nForEach * 4) * (MathsUtils.digamma(k) - 1.0 / (double)k - 2*MathsUtils.digamma(2*k+1+exclWindow + exclWindow-2) + MathsUtils.digamma(nForEach*4));
		
		MutualInfoCalculatorMultiVariateKraskov miCalc = getNewCalc(2);
		miCalc.setProperty(miCalc.PROP_K, Integer.toString(k));
		miCalc.setProperty(miCalc.PROP_ADD_NOISE, Integer.toString(0)); // Need to noise so that our analytic results hold

		// First check that for a simple single observation set everything works:
		miCalc.initialise(1, 1);
		miCalc.setObservations(x, y);
		double miWithoutDynCorrExcl = miCalc.computeAverageLocalOfObservations();
		assertEquals(expectedMIWithoutDynCorrExcl, miWithoutDynCorrExcl, 0.00001);

		// Now turn dynamic correlation exclusion on:
		miCalc.setProperty(miCalc.PROP_DYN_CORR_EXCL_TIME, Integer.toString(exclWindow));
		// miCalc.setProperty(miCalc.PROP_NUM_THREADS, Integer.toString(1)); // To simplify debugging
		miCalc.initialise(1, 1);
		miCalc.setObservations(x, y);
		double miWithDynCorrExcl = miCalc.computeAverageLocalOfObservations();
		assertEquals(expectedMIWithDynCorrExcl, miWithDynCorrExcl, 0.00001);

		// This time, we leave dynamic correlation exclusion turned on, but add each sample as
		// a separate data set -- this will serve to effectively turn dynamic correlation exclusion off again!
		// Gives a good test that exclusion only considers points within the same data set.
		miCalc.initialise(1, 1);
		miCalc.startAddObservations();
		for (int t = 0; t < x.length; t++) {
			miCalc.addObservations(new double[] {x[t]}, new double[] {y[t]});
		}
		miCalc.finaliseAddObservations();
		double miWithDynCorrExclAndSeperateSets = miCalc.computeAverageLocalOfObservations();
		assertEquals(expectedMIWithoutDynCorrExcl, miWithDynCorrExclAndSeperateSets, 0.00001);
	}

	/**
	 * Extends the tests from testUnivariateMIforRandomVariablesFromFile to use
	 *  a random seed for noise and check that results are repeatable.
	 * 
	 * @throws Exception if file not found 
	 * 
	 */
	public void testUnivariateMIWithSeed() throws Exception {
		
		// Test set 1:
		
		ArrayFileReader afr = new ArrayFileReader("demos/data/2randomCols-1.txt");
		double[][] data = afr.getDouble2DMatrix();
		
		// Use various Kraskov k nearest neighbours parameter
		int[] kNNs = {1, 2, 3, 4, 5, 6, 10, 15};
		// Expected values from Kraskov's MILCA toolkit:
		double[] expectedFromMILCA = {-0.05294175, -0.03944338, -0.02190217,
				0.00120807, -0.00924771, -0.00316402, -0.00778205, -0.00565778};
		
		System.out.println("Kraskov comparison 1 - univariate random data 1 - no seed");
		double[] noNoiseErrors = checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, expectedFromMILCA);
		double[] noNoiseResults = MatrixUtils.add(expectedFromMILCA, noNoiseErrors);
		
		// Check that changing the random number generator still returns close to those with no noise 
		//  results, but with larger tolerance
		System.out.println("\n Kraskov comparison 1 - univariate random data 1 - seed 1");
		double[] withSeed1Errors = checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, noNoiseResults,
				1e-8, "1", 0.01);
		double[] withSeed1Results = MatrixUtils.add(noNoiseResults, withSeed1Errors);
		// Now check that the results are exact when we repeat with the same seed:
		System.out.println("\n Kraskov comparison 1 - univariate random data 1 - seed 1 repeat");
		checkMIForGivenData(MatrixUtils.selectColumns(data, new int[] {0}),
				MatrixUtils.selectColumns(data, new int[] {1}),
				kNNs, withSeed1Results,
				1e-8, "1", 1e-10);
	}
}

