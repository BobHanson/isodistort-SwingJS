/* $RCSfile$
 * $Author: hansonr $
 * $Date: 2016-04-10 22:47:52 -0500 (Sun, 10 Apr 2016) $
 *
 * Copyright (C) 2003-2005  Miguel, Jmol Development, www.jmol.org
 *
 * Contact: jmol-developers@lists.sf.net
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.byu.isodistort.local;

import java.util.Iterator;

/**
 * Borrowed from Jmol's org.openscience.jmol.bspf.Bspf,
 * 
 * Contains inner private static classes Element, Leaf, and Node as well 
 * as public static class CubeIterator
 * 
 * 
 * <p>
 * a Binary Space Partitioning Tree
 * </p>
 * <p>
 * The tree partitions n-dimensional space (in our case 3) into little boxes,
 * facilitating searches for things which are *nearby*.
 * </p>
 * <p>
 * For some useful background info, search the web for "bsp tree faq". Our
 * application is somewhat simpler because we are storing points instead of
 * polygons.
 * </p>
 * <p>
 * We are working with three dimensions. For the purposes of the Bspt code these
 * dimensions are stored as 0, 1, or 2. Each node of the tree splits along the
 * next dimension, wrapping around to 0.
 * 
 * <pre>
 * mySplitDimension = (parentSplitDimension + 1) % 3;
 * </pre>
 * 
 * A split value is stored in the node. Values which are <= splitValue are
 * stored down the left branch. Values which are >= splitValue are stored down
 * the right branch. If searchValue == splitValue then the search must proceed
 * down both branches.
 * </p>
 * <p>
 * Planar and crystaline substructures can generate values which are == along
 * one dimension.
 * </p>
 * <p>
 * To get a good picture in your head, first think about it in one dimension,
 * points on a number line. The tree just partitions the points. Now think about
 * 2 dimensions. The first node of the tree splits the plane into two rectangles
 * along the x dimension. The second level of the tree splits the subplanes
 * (independently) along the y dimension into smaller rectangles. The third
 * level splits along the x dimension. In three dimensions, we are doing the
 * same thing, only working with 3-d boxes.
 * </p>
 * 
 * @author Miguel, miguel@jmol.org
 */

public class Bspt {

	CubeIterator cubeIterator;

	public boolean isValid;

	final static int leafCountMax = 2;
	// this corresponds to the max height of the tree
	final static int MAX_TREE_DEPTH = 100;
	int treeDepth;
	Element eleRoot;

	/**
	 * Create a bspt with the specified number of dimensions. For a 3-dimensional
	 * tree (x,y,z) call new Bspt(3).
	 * 
	 * @param dimMax
	 * @param index
	 */
	public Bspt() {
		reset();
	}

	void reset() {
		eleRoot = new Leaf(this, null, 0);
		treeDepth = 1;
	}

	public void validate(boolean isValid) {
		isValid = true;
	}

	public boolean isInitialized() {
		return isValid;
	}

	/**
	 * @return either a cached or a new CubeIterator
	 * 
	 */
	public CubeIterator getCubeIterator(boolean isNew) {
		if (isNew)
			return getNewCubeIterator();
		return (cubeIterator == null ? (cubeIterator = getNewCubeIterator()) : cubeIterator);
	}

	public CubeIterator getNewCubeIterator() {
		return allocateCubeIterator();
	}

	public synchronized void initialize(double[][] atoms) {
		reset();
		for (int i = 0, n = atoms.length; i < n; i++)
			addTuple(atoms[i]);
		isValid = true;
	}

	/**
	 * Iterate through all of your data points, calling addTuple
	 * 
	 * @param tuple
	 */
	public void addTuple(double[] tuple) {
		eleRoot = eleRoot.addTuple(0, tuple);
	}

	/**
	 * prints some simple stats to stdout
	 */
	public void stats() {
		// if (Logger.debugging) {
		// Logger.debug(
		// "bspt treeDepth=" + treeDepth +
		// " count=" + eleRoot.count);
		// }
	}

	// public void dump() {
//      SB sb = new SB();
//      eleRoot.dump(0, sb);
//      Logger.info(sb.toString());
	// }
	//
	// @Override
	// public String toString() {
	// return eleRoot.toString();
	// }

	public CubeIterator allocateCubeIterator() {
		return new CubeIterator(this);
	}

	/**
	 * Iterator used for finding all points within a box or a hemi-box
	 * <p>
	 * Obtain a CubeIterator by calling Bspt.allocateCubeIterator().
	 * <p>
	 * call initialize(...) or initializeHemizphere(...)
	 * <p>
	 * re-initialize in order to reuse the same CubeIterator
	 *
	 * @author Miguel, miguel@jmol.org
	 */
	public static class CubeIterator implements Iterator<double[]> {
		private Bspt bspt;

		private Element[] stack;
		private int sp;
		private int leafIndex;
		private Leaf leaf;
		private double radius;
		private double[] center = new double[3];
		private double dx, dy, dz;


		// (on the first dim) is returned
//		private boolean tHemisphere;

		CubeIterator(Bspt bspt) {
			set(bspt);
		}

		void set(Bspt bspt) {
			this.bspt = bspt;
			stack = new Element[bspt.treeDepth];
		}

		/**
		 * initialize to return all points within the sphere defined by center and
		 * radius
		 *
		 * @param center
		 * @param radius
		 * @param hemisphereOnly
		 */
		public void initialize(double[] center, double radius, boolean hemisphereOnly) {
			this.center = center;
			this.radius = radius;
//			tHemisphere = false;
			leaf = null;
			// allow dynamic allocation (Symmetry.getCrystalClass)
			if (stack.length < bspt.treeDepth)
				set(bspt);
			stack[0] = bspt.eleRoot;
			sp = 1;
			findLeftLeaf();
//			tHemisphere = hemisphereOnly;
		}

		/**
		 * nulls internal references
		 */
		public void release() {
			set(bspt);
		}

		/**
		 * normal iterator predicate
		 *
		 * @return boolean
		 */
		@Override
		public boolean hasNext() {
			while (leaf != null) {
				for (; leafIndex < leaf.count; ++leafIndex)
					if (isWithinRadius(leaf.tuples[leafIndex]))
						return true;
				findLeftLeaf();
			}
			return false;
		}

		/**
		 * normal iterator method
		 *
		 * @return Tuple
		 */
		@Override
		public double[] next() {
			return leaf.tuples[leafIndex++];
		}

		/**
		 * After calling nextElement(), allows one to find out the value of the distance
		 * squared. To get the distance just take the sqrt.
		 *
		 * @return double
		 */
		public double foundDistance2() {
			return dx * dx + dy * dy + dz * dz;
		}

		/**
		 * does the work
		 */
		private void findLeftLeaf() {
			leaf = null;
			if (sp == 0)
				return;
			Element ele = stack[--sp];
			while (ele instanceof Node) {
				Node node = (Node) ele;
				double minValue = center[node.dim];
				double maxValue = minValue + radius;
//				if (!tHemisphere || node.dim != 0)
					minValue -= radius;
				if (minValue <= node.maxLeft && maxValue >= node.minLeft) {
					if (maxValue >= node.minRight && minValue <= node.maxRight) {
						stack[sp++] = node.eleRight;
					}
					ele = node.eleLeft;
				} else if (maxValue >= node.minRight && minValue <= node.maxRight) {
					ele = node.eleRight;
				} else {
					if (sp == 0)
						return;
					ele = stack[--sp];
				}
			}
			leaf = (Leaf) ele;
			leafIndex = 0;
		}

		/**
		 * checks one Point for box-based distance
		 * 
		 * @param t
		 * @return boolean
		 */
		private boolean isWithinRadius(double[] t) {
//			if (center[3] == 14)// && t[3] == 38)
//				System.out.println(center[3] + " " + t[3] + " "  + MathUtil.dist3(t,  center));
			if (t[3] <= center[3])
				return false;
			dx = t[0] - center[0];
			return ((dx = Math.abs(dx)) <= radius
					&& (dy = Math.abs(t[1] - center[1])) <= radius 
					&& (dz = Math.abs(t[2] - center[2])) <= radius);
		}

	}
	/**
	 * the internal tree is made up of elements ... either Node or Leaf
	 *
	 * @author Miguel, miguel@jmol.org
	 */
	private abstract static class Element {
		Bspt bspt;
		int count;

		abstract Element addTuple(int level, double[] tuple);
	}

	/**
	 * A leaf of Point3f objects in the bsp tree
	 *
	 * @author Miguel, miguel@jmol.org
	 */
	private static class Leaf extends Element {
		double[][] tuples;

		/**
		 * @param bspt
		 * @param leaf
		 * @param countToKeep
		 * 
		 */
		Leaf(Bspt bspt, Leaf leaf, int countToKeep) {
			this.bspt = bspt;
			count = 0;
			tuples = new double[Bspt.leafCountMax][];
			if (leaf == null)
				return;
			for (int i = countToKeep; i < Bspt.leafCountMax; ++i) {
				tuples[count++] = leaf.tuples[i];
				leaf.tuples[i] = null;
			}
			leaf.count = countToKeep;
		}

		void sort(int dim) {
			for (int i = count; --i > 0;) { // this is > not >=
				double[] champion = tuples[i];
				double championValue = champion[dim];
				for (int j = i; --j >= 0;) {
					double[] challenger = tuples[j];
					double challengerValue = challenger[dim];
					if (challengerValue > championValue) {
						tuples[i] = challenger;
						tuples[j] = champion;
						champion = challenger;
						championValue = challengerValue;
					}
				}
			}
		}

		@Override
		Element addTuple(int level, double[] tuple) {
			if (count < Bspt.leafCountMax) {
				tuples[count++] = tuple;
				return this;
			}
			Node node = new Node(bspt, level, this);
			return node.addTuple(level, tuple);
		}

		// @Override
		// void dump(int level, SB sb) {
//        for (int i = 0; i < count; ++i) {
//          T3 t = tuples[i];
//          for (int j = 0; j < level; ++j)
//            sb.append(".");
//          sb.append(Escape.eP(t)).append("Leaf ").appendI(i).append(": ").append(((Atom) t).getInfo());
//        }
		// }

		// @Override
		// public String toString() {
//        return "leaf:" + count + "\n";
		// }

	}

	private static class Node extends Element {
		int dim;
		double minLeft, maxLeft;
		Element eleLeft;
		double minRight, maxRight;
		Element eleRight;

		/**
		 * @param bspt
		 * @param level
		 * @param leafLeft
		 * 
		 */
		Node(Bspt bspt, int level, Leaf leafLeft) {
			this.bspt = bspt;
			if (level == bspt.treeDepth) {
				bspt.treeDepth = level + 1;
				// no longer necessary -- in a long unfolded protein,
				// we can go over 100 here
				// if (bspt.treeDepth >= Bspt.MAX_TREE_DEPTH)
				// Logger.error("BSPT tree depth too great:" + bspt.treeDepth);
			}
			if (leafLeft.count != Bspt.leafCountMax)
				throw new NullPointerException();
			dim = level % 3;
			leafLeft.sort(dim);
			Leaf leafRight = new Leaf(bspt, leafLeft, Bspt.leafCountMax / 2);
			minLeft = leafLeft.tuples[0][dim];
			maxLeft = leafLeft.tuples[leafLeft.count - 1][dim];
			minRight = leafRight.tuples[0][dim];
			maxRight = leafRight.tuples[leafRight.count - 1][dim];

			eleLeft = leafLeft;
			eleRight = leafRight;
			count = Bspt.leafCountMax;
		}

		@Override
		Element addTuple(int level, double[] tuple) {
			double dimValue = tuple[dim];
			++count;
			boolean addLeft;
			if (dimValue < maxLeft) {
				addLeft = true;
			} else if (dimValue > minRight) {
				addLeft = false;
			} else if (dimValue == maxLeft) {
				if (dimValue == minRight) {
					if (eleLeft.count < eleRight.count)
						addLeft = true;
					else
						addLeft = false;
				} else {
					addLeft = true;
				}
			} else if (dimValue == minRight) {
				addLeft = false;
			} else {
				if (eleLeft.count < eleRight.count)
					addLeft = true;
				else
					addLeft = false;
			}
			if (addLeft) {
				if (dimValue < minLeft)
					minLeft = dimValue;
				else if (dimValue > maxLeft)
					maxLeft = dimValue;
				eleLeft = eleLeft.addTuple(level + 1, tuple);
			} else {
				if (dimValue < minRight)
					minRight = dimValue;
				else if (dimValue > maxRight)
					maxRight = dimValue;
				eleRight = eleRight.addTuple(level + 1, tuple);
			}
			return this;
		}

		// @Override
		// void dump(int level, SB sb) {
//    	    sb.append("\nnode LEFT" + level);
//    	    eleLeft.dump(level + 1, sb);
//    	    for (int i = 0; i < level; ++i)
//    	    sb.append("->");
//    	    sb.append(" RIGHT" + level);
//    	    eleRight.dump(level + 1, sb);
//    	    }

//    	    @Override
//    	    public String toString() {
//    	      return eleLeft.toString() + dim + ":" + "\n" + eleRight.toString();
//    	    }

	}

}
