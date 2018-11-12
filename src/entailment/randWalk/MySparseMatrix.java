package entailment.randWalk;

import java.util.ArrayList;

/*******************************************************************************
 * Copyright (c) 2010 Haifeng Li
 *   
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.google.common.collect.BiMap;

import smile.math.Math;
import smile.math.matrix.Matrix;
import smile.math.matrix.MatrixMultiplication;
import smile.math.matrix.SparseMatrix;

/**
 * A sparse matrix is a matrix populated primarily with zeros. Conceptually,
 * sparsity corresponds to systems which are loosely coupled. Huge sparse
 * matrices often appear when solving partial differential equations.
 * <p>
 * Operations using standard dense matrix structures and algorithms are slow and
 * consume large amounts of memory when applied to large sparse matrices.
 * Indeed, some very large sparse matrices are infeasible to manipulate with the
 * standard dense algorithms. Sparse data is by nature easily compressed, and
 * this compression almost always results in significantly less computer data
 * storage usage.
 * <p>
 * This class employs Harwell-Boeing column-compressed sparse matrix format.
 * Nonzero values are stored in an array (top-to-bottom, then
 * left-to-right-bottom). The row indices corresponding to the values are also
 * stored. Besides, a list of pointers are indexes where each column starts.
 * This format is efficient for arithmetic operations, column slicing, and
 * matrix-vector products. One typically uses SparseDataset for construction of
 * SparseMatrix.
 *
 * @author Haifeng Li
 */
public class MySparseMatrix extends Matrix implements MatrixMultiplication<SparseMatrix, SparseMatrix> {
	private static final long serialVersionUID = 1L;

	/**
	 * The number of rows.
	 */
	private int nrows;
	/**
	 * The number of columns.
	 */
	private int ncols;
	/**
	 * The index of the start of columns.
	 */
	int[] colIndex;
	/**
	 * The row indices of nonzero values.
	 */
	int[] rowIndex;
	/**
	 * The array of nonzero values stored column by column.
	 */
	private double[] x;

	/**
	 * Constructor.
	 * 
	 * @param nrows
	 *            the number of rows in the matrix.
	 * @param ncols
	 *            the number of columns in the matrix.
	 * @param nvals
	 *            the number of nonzero entries in the matrix.
	 */
	private MySparseMatrix(int nrows, int ncols, int nvals) {
		this.nrows = nrows;
		this.ncols = ncols;
		rowIndex = new int[nvals];
		colIndex = new int[ncols + 1];
		x = new double[nvals];
	}

	/**
	 * Constructor.
	 * 
	 * @param nrows
	 *            the number of rows in the matrix.
	 * @param ncols
	 *            the number of columns in the matrix.
	 * @param rowIndex
	 *            the row indices of nonzero values.
	 * @param colIndex
	 *            the index of the start of columns.
	 * @param x
	 *            the array of nonzero values stored column by column.
	 */

	public MySparseMatrix(int nrows, int ncols, double[] x, int[] rowIndex, int[] colIndex) {
		this.nrows = nrows;
		this.ncols = ncols;
		this.rowIndex = rowIndex;
		this.colIndex = colIndex;
		this.x = x;
	}

	/**
	 * Constructor.
	 * 
	 * @param D
	 *            a dense matrix to converted into sparse matrix format.
	 */
	public MySparseMatrix(double[][] D) {
		this(D, 100 * Math.EPSILON);
	}

	/**
	 * Constructor.
	 * 
	 * @param D
	 *            a dense matrix to converted into sparse matrix format.
	 * @param tol
	 *            the tolerance to regard a value as zero if |x| &lt; tol.
	 */
	public MySparseMatrix(double[][] D, double tol) {
		nrows = D.length;
		ncols = D[0].length;

		int n = 0; // number of non-zero elements
		for (int i = 0; i < nrows; i++) {
			for (int j = 0; j < ncols; j++) {
				if (Math.abs(D[i][j]) >= tol) {
					n++;
				}
			}
		}

		x = new double[n];
		rowIndex = new int[n];
		colIndex = new int[ncols + 1];
		colIndex[ncols] = n;

		n = 0;
		for (int j = 0; j < ncols; j++) {
			colIndex[j] = n;
			for (int i = 0; i < nrows; i++) {
				if (Math.abs(D[i][j]) >= tol) {
					rowIndex[n] = i;
					x[n] = D[i][j];
					n++;
				}
			}
		}
	}

	@Override
	public int nrows() {
		return nrows;
	}

	@Override
	public int ncols() {
		return ncols;
	}

	/**
	 * Returns the number of nonzero values.
	 */
	public int size() {
		return colIndex[ncols];
	}

	/**
	 * Returns all nonzero values.
	 * 
	 * @return all nonzero values
	 */
	public double[] values() {
		return x;
	}

	@Override
	public double get(int i, int j) {
		if (i < 0 || i >= nrows || j < 0 || j >= ncols) {
			throw new IllegalArgumentException("Invalid index: i = " + i + " j = " + j);
		}

		for (int k = colIndex[j]; k < colIndex[j + 1]; k++) {
			if (rowIndex[k] == i) {
				return x[k];
			}
		}

		return 0.0;
	}

	@Override
	public double[] ax(double[] x, double[] y) {
		Arrays.fill(y, 0.0);

		for (int j = 0; j < ncols; j++) {
			for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
				y[rowIndex[i]] += this.x[i] * x[j];
			}
		}

		return y;
	}

	@Override
	public double[] axpy(double[] x, double[] y) {
		for (int j = 0; j < ncols; j++) {
			for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
				y[rowIndex[i]] += this.x[i] * x[j];
			}
		}

		return y;
	}

	@Override
	public double[] axpy(double[] x, double[] y, double b) {
		for (int i = 0; i < y.length; i++) {
			y[i] *= b;
		}

		for (int j = 0; j < ncols; j++) {
			for (int i = colIndex[j]; i < colIndex[j + 1]; i++) {
				y[rowIndex[i]] += this.x[i] * x[j];
			}
		}

		return y;
	}

	@Override
	public double[] atx(double[] x, double[] y) {
		Arrays.fill(y, 0.0);
		for (int i = 0; i < ncols; i++) {
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				y[i] += this.x[j] * x[rowIndex[j]];
			}
		}

		return y;
	}

	@Override
	public MySparseMatrix transpose() {
		int m = nrows, n = ncols;
		MySparseMatrix at = new MySparseMatrix(n, m, x.length);

		int[] count = new int[m];
		for (int i = 0; i < n; i++) {
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				int k = rowIndex[j];
				count[k]++;
			}
		}

		for (int j = 0; j < m; j++) {
			at.colIndex[j + 1] = at.colIndex[j] + count[j];
		}

		Arrays.fill(count, 0);
		for (int i = 0; i < n; i++) {
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				int k = rowIndex[j];
				int index = at.colIndex[k] + count[k];
				at.rowIndex[index] = i;
				at.x[index] = x[j];
				count[k]++;
			}
		}

		return at;
	}

	@Override
	public double[] atxpy(double[] x, double[] y) {
		for (int i = 0; i < ncols; i++) {
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				y[i] += this.x[j] * x[rowIndex[j]];
			}
		}

		return y;
	}

	@Override
	public double[] atxpy(double[] x, double[] y, double b) {
		for (int i = 0; i < ncols; i++) {
			y[i] *= b;
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				y[i] += this.x[j] * x[rowIndex[j]];
			}
		}

		return y;
	}

	/**
	 * Returns the matrix multiplication C = A * B.
	 */
	public MySparseMatrix abmm(MySparseMatrix B) {
		if (ncols != B.nrows) {
			throw new IllegalArgumentException(
					String.format("Matrix dimensions do not match for matrix multiplication: %d x %d vs %d x %d",
							nrows(), ncols(), B.nrows(), B.ncols()));
		}

		int m = nrows;
		System.out.println("m: " + m);
		int anz = size();
		int n = B.ncols;
		int[] Bp = B.colIndex;
		int[] Bi = B.rowIndex;
		double[] Bx = B.x;
		int bnz = Bp[n];

		int[] w = new int[m];
		double[] abj = new double[m];

		int nzmax = Math.max(anz + bnz, m);

		MySparseMatrix C = new MySparseMatrix(m, n, nzmax);
		int[] Cp = C.colIndex;
		int[] Ci = C.rowIndex;
		double[] Cx = C.x;

		int nz = 0;
		for (int j = 0; j < n; j++) {
			if (nz + m > nzmax) {
				System.out.println("nzmax0: " + nzmax);
				nzmax = 2 * nzmax + m;
				System.out.println("nzmax: " + nzmax);
				double[] Cx2 = new double[nzmax];
				int[] Ci2 = new int[nzmax];
				System.arraycopy(Ci, 0, Ci2, 0, nz);
				System.arraycopy(Cx, 0, Cx2, 0, nz);
				Ci = Ci2;
				Cx = Cx2;
				C.rowIndex = Ci;
				C.x = Cx;
			}

			// column j of C starts here
			Cp[j] = nz;

			for (int p = Bp[j]; p < Bp[j + 1]; p++) {
				nz = scatter(this, Bi[p], Bx[p], w, abj, j + 1, C, nz);
			}

			for (int p = Cp[j]; p < nz; p++) {
				Cx[p] = abj[Ci[p]];
			}
		}

		// finalize the last column of C
		Cp[n] = nz;

		return C;
	}

	/**
	 * x = x + beta * A(:,j), where x is a dense vector and A(:,j) is sparse.
	 */
	private static int scatter(MySparseMatrix A, int j, double beta, int[] w, double[] x, int mark, MySparseMatrix C,
			int nz) {
		int[] Ap = A.colIndex;
		int[] Ai = A.rowIndex;
		double[] Ax = A.x;

		int[] Ci = C.rowIndex;
		for (int p = Ap[j]; p < Ap[j + 1]; p++) {
			int i = Ai[p]; // A(i,j) is nonzero
			if (w[i] < mark) {
				w[i] = mark; // i is new entry in column j
				Ci[nz++] = i; // add i to pattern of C(:,j)
				x[i] = beta * Ax[p]; // x(i) = beta*A(i,j)
			} else {
				x[i] += beta * Ax[p]; // i exists in C(:,j) already
			}
		}

		return nz;
	}

	@Override
	public SparseMatrix abtmm(SparseMatrix B) {
		SparseMatrix BT = B.transpose();
		return abmm(BT);
	}

	public MySparseMatrix atbmm(MySparseMatrix B) {
		MySparseMatrix AT = transpose();
		return AT.abmm(B);
	}

	public MySparseMatrix ata() {
		MySparseMatrix AT = transpose();
		return AT.aat(this);
	}

	public MySparseMatrix aat() {
		MySparseMatrix AT = transpose();
		return aat(AT);
	}

	private MySparseMatrix aat(MySparseMatrix AT) {
		int m = nrows;
		int[] done = new int[m];
		for (int i = 0; i < m; i++) {
			done[i] = -1;
		}

		// First pass determines the number of nonzeros.
		int nvals = 0;
		// Outer loop over columns of A' in AA'
		for (int j = 0; j < m; j++) {
			for (int i = AT.colIndex[j]; i < AT.colIndex[j + 1]; i++) {
				int k = AT.rowIndex[i];
				for (int l = colIndex[k]; l < colIndex[k + 1]; l++) {
					int h = rowIndex[l];
					// Test if contribution already included.
					if (done[h] != j) {
						done[h] = j;
						nvals++;
					}
				}
			}
		}

		MySparseMatrix aat = new MySparseMatrix(m, m, nvals);

		nvals = 0;
		for (int i = 0; i < m; i++) {
			done[i] = -1;
		}

		// Second pass determines columns of aat. Code is identical to first
		// pass except colIndex and rowIndex get assigned at appropriate places.
		for (int j = 0; j < m; j++) {
			aat.colIndex[j] = nvals;
			for (int i = AT.colIndex[j]; i < AT.colIndex[j + 1]; i++) {
				int k = AT.rowIndex[i];
				for (int l = colIndex[k]; l < colIndex[k + 1]; l++) {
					int h = rowIndex[l];
					if (done[h] != j) {
						done[h] = j;
						aat.rowIndex[nvals] = h;
						nvals++;
					}
				}
			}
		}

		// Set last value.
		aat.colIndex[m] = nvals;

		// Sort columns.
		for (int j = 0; j < m; j++) {
			if (aat.colIndex[j + 1] - aat.colIndex[j] > 1) {
				Arrays.sort(aat.rowIndex, aat.colIndex[j], aat.colIndex[j + 1]);
			}
		}

		double[] temp = new double[m];
		for (int i = 0; i < m; i++) {
			for (int j = AT.colIndex[i]; j < AT.colIndex[i + 1]; j++) {
				int k = AT.rowIndex[j];
				for (int l = colIndex[k]; l < colIndex[k + 1]; l++) {
					int h = rowIndex[l];
					temp[h] += AT.x[j] * x[l];
				}
			}

			for (int j = aat.colIndex[i]; j < aat.colIndex[i + 1]; j++) {
				int k = aat.rowIndex[j];
				aat.x[j] = temp[k];
				temp[k] = 0.0;
			}
		}

		return aat;
	}

	@Override
	public double[] diag() {
		int n = smile.math.Math.min(nrows(), ncols());
		double[] d = new double[n];

		for (int i = 0; i < n; i++) {
			for (int j = colIndex[i]; j < colIndex[i + 1]; j++) {
				if (rowIndex[j] == i) {
					d[i] = x[j];
					break;
				}
			}
		}

		return d;
	}

	@Override
	public SparseMatrix abmm(SparseMatrix arg0) {
		return null;
	}

	@Override
	public SparseMatrix atbmm(SparseMatrix arg0) {
		// TODO Auto-generated method stub
		return null;
	}

	// threshold anything other than the first N elements
	public MySparseMatrix threshold(int N) {
		if (N >= x.length) {
			return threshold(-1.0);
		}

		double[] y = Arrays.copyOf(x, x.length);
		Arrays.sort(y);
		double thr = y[y.length - N];
		System.out.println(N + "th: " + thr);
		return threshold(thr);

	}

	public MySparseMatrix threshold(double threshold) {
		List<Double> newx = new ArrayList<>();
		List<Integer> newRowIndex = new ArrayList<>();
		int[] newColIndex = new int[colIndex.length];

		for (int c = 0; c < colIndex.length - 1; c++) {
			newColIndex[c] = newx.size();
			for (int i = colIndex[c]; i < colIndex[c + 1]; i++) {
				if (x[i] >= threshold) {
					newx.add(x[i]);
					newRowIndex.add(rowIndex[i]);
				}
			}
		}

		newColIndex[colIndex.length - 1] = newx.size();

		MySparseMatrix mat = new MySparseMatrix(nrows, ncols, newx.stream().mapToDouble(d -> d).toArray(),
				newRowIndex.stream().mapToInt(i -> i).toArray(), newColIndex);
		return mat;
	}

	public MySparseMatrix removeHubs(Set<String> hubs, BiMap<String, Integer> row2idx, BiMap<String, Integer> col2idx) {
		List<Double> newx = new ArrayList<>();
		List<Integer> newRowIndex = new ArrayList<>();
		int[] newColIndex = new int[colIndex.length];

		for (int c = 0; c < colIndex.length - 1; c++) {
			newColIndex[c] = newx.size();
			if (hubs.contains(col2idx.inverse().get(c))) {
				// System.out.println("removing hub col"+col2idx.inverse().get(c));
				continue;
			}

			for (int i = colIndex[c]; i < colIndex[c + 1]; i++) {
				if (!hubs.contains(row2idx.inverse().get(rowIndex[i]))) {
					newx.add(x[i]);
					newRowIndex.add(rowIndex[i]);
				} else {
					// System.out.println("removing hub row: "+row2idx.inverse().get(rowIndex[i]));
				}
			}
		}

		newColIndex[colIndex.length - 1] = newx.size();

		MySparseMatrix mat = new MySparseMatrix(nrows, ncols, newx.stream().mapToDouble(d -> d).toArray(),
				newRowIndex.stream().mapToInt(i -> i).toArray(), newColIndex);
		return mat;
	}

	// submatrix for 0 to r-1 indices
	// public MySparseMatrix subMatrix(int r) {
	// double[] newx = new double[colIndex[r]];
	// int[] newRowIndex = new int[colIndex[r]];
	// int[] newColIndex = new int[r+1];
	//
	// for (int c=0; c<r; c++) {
	// newColIndex[c] = colIndex[c];
	// for (int i=colIndex[c]; i<colIndex[c+1]; i++) {
	//
	// newx[i] = x[i];
	// newRowIndex[i] = rowIndex[i];
	//
	// }
	// }
	//
	// newColIndex[r] = colIndex[r];
	//
	// MySparseMatrix mat = new MySparseMatrix(r, r, newx, newRowIndex,
	// newColIndex);
	// return mat;
	// }

	// A(i,j) = min(1,A(i,j)/A(i,i))
	
	public MySparseMatrix normalizeRows() {
		double[] newx = new double[x.length];
		int[] newRowIndex = new int[rowIndex.length];
		int[] newColIndex = new int[colIndex.length];

		double[] diag = diag();

		for (int c = 0; c < colIndex.length - 1; c++) {
			newColIndex[c] = colIndex[c];

			for (int i = colIndex[c]; i < colIndex[c + 1]; i++) {
				try {
					newx[i] = Math.min(1,(x[i]/diag[rowIndex[i]]));
				} catch (Exception e) {
					newx[i] = x[i];
				}
				newRowIndex[i] = rowIndex[i];
			}
		}

		newColIndex[colIndex.length - 1] = colIndex[colIndex.length-1];

		MySparseMatrix mat = new MySparseMatrix(nrows, ncols, newx,
				newRowIndex, newColIndex);
		return mat;
	}
}
