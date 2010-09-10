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
package net.sf.mzmine.modules.peaklistmethods.io.csvexport;

import java.io.FileWriter;
import net.sf.mzmine.data.ChromatographicPeak;
import net.sf.mzmine.data.PeakIdentity;
import net.sf.mzmine.data.PeakList;
import net.sf.mzmine.data.PeakListRow;
import net.sf.mzmine.data.PeakStatus;
import net.sf.mzmine.data.RawDataFile;
import net.sf.mzmine.taskcontrol.AbstractTask;
import net.sf.mzmine.taskcontrol.TaskStatus;
import net.sf.mzmine.util.CollectionUtils;

class CSVExportTask extends AbstractTask {

	private PeakList peakList;
	private int processedRows, totalRows;

	// parameter values
	private String fileName, fieldSeparator;
	private ExportRowCommonElement[] commonElements;
	private String[] identityElements;
	private ExportRowDataFileElement[] dataFileElements;

	CSVExportTask(PeakList peakList, CSVExporterParameters parameters) {

		this.peakList = peakList;

		fileName = (String) parameters
				.getParameterValue(CSVExporterParameters.filename);
		fieldSeparator = (String) parameters
				.getParameterValue(CSVExporterParameters.fieldSeparator);

		Object commonElementsObjects[] = (Object[]) parameters
				.getParameterValue(CSVExporterParameters.exportCommonItemMultipleSelection);
		commonElements = CollectionUtils.changeArrayType(commonElementsObjects,
				ExportRowCommonElement.class);

		Object identityElementsObjects[] = (Object[]) parameters
				.getParameterValue(CSVExporterParameters.exportIdentityItemMultipleSelection);
		identityElements = CollectionUtils.changeArrayType(
				identityElementsObjects, String.class);

		Object dataFileElementsObjects[] = (Object[]) parameters
				.getParameterValue(CSVExporterParameters.exportDataFileItemMultipleSelection);
		dataFileElements = CollectionUtils.changeArrayType(
				dataFileElementsObjects, ExportRowDataFileElement.class);

	}

	public double getFinishedPercentage() {
		if (totalRows == 0) {
			return 0;
		}
		return (double) processedRows / (double) totalRows;
	}

	public String getTaskDescription() {
		return "Exporting peak list " + peakList + " to " + fileName;
	}

	public void run() {

		// Open file
		FileWriter writer;
		try {
			writer = new FileWriter(fileName);
		} catch (Exception e) {
			setStatus( TaskStatus.ERROR );
			errorMessage = "Could not open file " + fileName + " for writing.";
			return;
		}

		// Get number of rows
		totalRows = peakList.getNumberOfRows();

		RawDataFile rawDataFiles[] = peakList.getRawDataFiles();

		// Buffer for writing
		StringBuffer line = new StringBuffer();

		// Write column headers
		// Common elements
		int length = commonElements.length;
		String name;
		for (int i = 0; i < length; i++) {
			name = commonElements[i].getName();
			name = name.replace("Export ", "");
			line.append(name + fieldSeparator);
		}

		// Peak identity elements
		length = identityElements.length;
		for (int i = 0; i < length; i++) {
			name = identityElements[i];
			line.append(name + fieldSeparator);
		}

		// Data file elements
		length = dataFileElements.length;
		for (int df = 0; df < peakList.getNumberOfRawDataFiles(); df++) {
			for (int i = 0; i < length; i++) {
				name = dataFileElements[i].getName();
				name = name.replace("Export", rawDataFiles[df].getName());
				line.append(name + fieldSeparator);
			}
		}

		line.append("\n");

		try {
			writer.write(line.toString());
		} catch (Exception e) {
			setStatus( TaskStatus.ERROR );
			errorMessage = "Could not write to file " + fileName;
			return;
		}

		// Write data rows
		for (PeakListRow peakListRow : peakList.getRows()) {

			// Cancel?
			if ( isCanceled( )) {
				return;
			}

			// Reset the buffer
			line.setLength(0);

			// Common elements
			length = commonElements.length;
			for (int i = 0; i < length; i++) {
				switch (commonElements[i]) {
				case ROW_ID:
					line.append(peakListRow.getID() + fieldSeparator);
					break;
				case ROW_MZ:
					line.append(peakListRow.getAverageMZ() + fieldSeparator);
					break;
				case ROW_RT:
					line.append((peakListRow.getAverageRT() / 60)
							+ fieldSeparator);
					break;
				case ROW_COMMENT:
					if (peakListRow.getComment() == null) {
						line.append(fieldSeparator);
					} else {
						line.append(peakListRow.getComment() + fieldSeparator);
					}
					break;
				case ROW_PEAK_NUMBER:
					int numDetected = 0;
					for (ChromatographicPeak p : peakListRow.getPeaks()) {
						if (p.getPeakStatus() == PeakStatus.DETECTED) {
							numDetected++;
						}
					}
					line.append(numDetected + fieldSeparator);
					break;
				}
			}

			// Identity elements
			length = identityElements.length;
			PeakIdentity peakIdentity = peakListRow.getPreferredPeakIdentity();
			if (peakIdentity != null) {
				for (int i = 0; i < length; i++) {
					String propertyValue = peakIdentity
							.getPropertyValue(identityElements[i]);
					if (propertyValue == null) {
						propertyValue = "";
					}
					line.append(propertyValue + fieldSeparator);
				}
			} else {
				for (int i = 0; i < length; i++) {
					line.append(fieldSeparator);
				}				
			}

			// Data file elements
			length = dataFileElements.length;
			for (RawDataFile dataFile : rawDataFiles) {
				for (int i = 0; i < length; i++) {
					ChromatographicPeak peak = peakListRow.getPeak(dataFile);
					if (peak != null) {
						switch (dataFileElements[i]) {
						case PEAK_STATUS:
							line.append(peak.getPeakStatus() + fieldSeparator);
							break;
						case PEAK_MZ:
							line.append(peak.getMZ() + fieldSeparator);
							break;
						case PEAK_RT:
							line.append(peak.getRT() + fieldSeparator);
							break;
						case PEAK_HEIGHT:
							line.append(peak.getHeight() + fieldSeparator);
							break;
						case PEAK_AREA:
							line.append(peak.getArea() + fieldSeparator);
							break;
						}
					} else {
						line.append("N/A" + fieldSeparator);
					}
				}
			}

			line.append("\n");

			try {
				writer.write(line.toString());
			} catch (Exception e) {
				setStatus( TaskStatus.ERROR );
				errorMessage = "Could not write to file " + fileName;
				return;
			}

			processedRows++;

		}

		// Close file
		try {
			writer.close();
		} catch (Exception e) {
			setStatus( TaskStatus.ERROR );
			errorMessage = "Could not close file " + fileName;
			return;
		}

		setStatus( TaskStatus.FINISHED );

	}

	public Object[] getCreatedObjects() {
		return null;
	}
}
