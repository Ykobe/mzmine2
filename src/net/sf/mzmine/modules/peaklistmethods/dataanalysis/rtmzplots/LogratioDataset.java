/*
 * Copyright 2006-2010 The MZmine 2 Development Team
 *
 * This file is part of MZmine 2.
 *
 * MZmine 2 is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * MZmine 2 is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * MZmine 2; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.peaklistmethods.dataanalysis.rtmzplots;

import java.util.Vector;
import java.util.logging.Logger;

import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.data.impl.SimpleParameterSet;
import net.sf.mzmine.util.CollectionUtils;
import net.sf.mzmine.util.MathUtils;

import org.jfree.data.xy.AbstractXYZDataset;

public class LogratioDataset extends AbstractXYZDataset implements RTMZDataset  {

	private Logger logger = Logger.getLogger(this.getClass().getName());
	
	private double[] xCoords = new double[0];
	private double[] yCoords = new double[0];
	private double[] colorCoords = new double[0];
	private PeakListRow[] peakListRows = new PeakListRow[0];
	
	private String datasetTitle;
	
	public LogratioDataset(PeakList alignedPeakList, RawDataFile[] groupOneSelectedFiles, RawDataFile[] groupTwoSelectedFiles, SimpleParameterSet parameters) {
		int numOfRows = alignedPeakList.getNumberOfRows();
		
		boolean useArea = true;
		if (parameters.getParameterValue(RTMZAnalyzer.MeasurementType)==RTMZAnalyzer.MeasurementTypeHeight)
			useArea = false;
			
		// Generate title for the dataset
		datasetTitle = "Logratio analysis";
		datasetTitle = datasetTitle.concat(" (");
		if (useArea) 
			datasetTitle = datasetTitle.concat("Logratio of average peak areas");
		else
			datasetTitle = datasetTitle.concat("Logratio of average peak heights");
		datasetTitle = datasetTitle.concat(" in " + groupOneSelectedFiles.length + " vs. " + groupTwoSelectedFiles.length + " files");
		datasetTitle = datasetTitle.concat(")");
		logger.finest("Computing: " + datasetTitle);

		Vector<Double> xCoordsV = new Vector<Double>();
		Vector<Double> yCoordsV = new Vector<Double>();
		Vector<Double> colorCoordsV = new Vector<Double>();
		Vector<PeakListRow> peakListRowsV = new Vector<PeakListRow>();
		
		for (int rowIndex=0; rowIndex<numOfRows; rowIndex++) {
			
			PeakListRow row = alignedPeakList.getRow(rowIndex);
			
			// Collect available peak intensities for selected files
			Vector<Double> groupOnePeakIntensities = new Vector<Double>(); 
			for (int fileIndex=0; fileIndex<groupOneSelectedFiles.length; fileIndex++) {
				ChromatographicPeak p = row.getPeak(groupOneSelectedFiles[fileIndex]);
				if (p!=null) {
					if (useArea)
						groupOnePeakIntensities.add(p.getArea());
					else 
						groupOnePeakIntensities.add(p.getHeight());
				}
			}
			Vector<Double> groupTwoPeakIntensities = new Vector<Double>(); 
			for (int fileIndex=0; fileIndex<groupTwoSelectedFiles.length; fileIndex++) {
				ChromatographicPeak p = row.getPeak(groupTwoSelectedFiles[fileIndex]);
				if (p!=null) {
					if (useArea)
						groupTwoPeakIntensities.add(p.getArea());
					else 
						groupTwoPeakIntensities.add(p.getHeight());
				}
			}
			
			// If there are at least one measurement from each group for this peak then calc logratio and include this peak in the plot
			if ( (groupOnePeakIntensities.size()>0) && 
					(groupTwoPeakIntensities.size()>0) ) {
				
				double[] groupOneInts = CollectionUtils.toDoubleArray(groupOnePeakIntensities);
				double groupOneAvg = MathUtils.calcAvg(groupOneInts);
				double[] groupTwoInts = CollectionUtils.toDoubleArray(groupTwoPeakIntensities);
				double groupTwoAvg = MathUtils.calcAvg(groupTwoInts);
				double logratio = Double.NaN;
				if (groupTwoAvg!=0.0)
					logratio = (double) (Math.log(groupOneAvg/groupTwoAvg) / Math.log(2.0)); 
				
				Double rt = row.getAverageRT();
				Double mz = row.getAverageMZ();
				
				xCoordsV.add(rt);
				yCoordsV.add(mz);
				colorCoordsV.add(logratio);
				peakListRowsV.add(row);
				
			} 
	
		}

		// Finally store all collected values in arrays
		xCoords = CollectionUtils.toDoubleArray(xCoordsV);
		yCoords = CollectionUtils.toDoubleArray(yCoordsV);
		colorCoords = CollectionUtils.toDoubleArray(colorCoordsV);
		peakListRows = peakListRowsV.toArray(new PeakListRow[0]);
		
	}
	
	public String toString() {
		return datasetTitle;
	}	
	
	@Override
	public int getSeriesCount() {
		return 1;
	}

	@Override
	public Comparable getSeriesKey(int series) {
		if (series==0) return new Integer(1); else return null;
	}

	public Number getZ(int series, int item) {
		if (series!=0) return null;
		if ((colorCoords.length-1)<item) return null;
		return colorCoords[item];
	}

	public int getItemCount(int series) {
		return xCoords.length;
	}

	public Number getX(int series, int item) {
		if (series!=0) return null;
		if ((xCoords.length-1)<item) return null;
		return xCoords[item];
	}

	public Number getY(int series, int item) {
		if (series!=0) return null;
		if ((yCoords.length-1)<item) return null;
		return yCoords[item];	
	}
	
	public PeakListRow getPeakListRow(int item) {
		return peakListRows[item];
	}

}