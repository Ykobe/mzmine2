/*
 * Copyright 2006-2008 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * MZmine; if not, write to the Free Software Foundation, Inc., 51 Franklin St,
 * Fifth Floor, Boston, MA 02110-1301 USA
 */

package net.sf.mzmine.modules.identification.qbixlipiddb;

import java.util.ArrayList;

import net.sf.mzmine.data.CompoundIdentity;
import net.sf.mzmine.data.PeakListRow;

/**
 * This class implements methods needed in pre- and post-processing queries and responses
 */
class QBIXLipidDBUtils {

	/*
	 * Preprocessing table
	 */

	private final int lctColMinMZ = 0;
	private final int lctColMaxMZ = 1;
	private final int lctColMinRT = 2;
	private final int lctColMaxRT = 3;
	private final int lctColName =  4;
	private final int lctColAdd = 5;
	private final int lctColAdduct = 6;
	private final int lctColExpected = 7;
	
	private final Object[][] lipidClassTable = {			
{0.0f,		650.0f,				0.0f,		300.0f, 			"Glycerophospholipi*",	1.007825f,				"[M+H]",		"LPC/LPE/LPA/LSer"},
{0.0f,		500.0f, 			0.0f, 		300.0f,				"Glycerolipids", 		-17.0027f,				"[(M+H)-18]",	"MAG"},
{550.0f,	Float.MAX_VALUE,	300.0f,		420.0f, 			"Glycerophospholipi*", 	1.007825f,				"[M+H]",		"GPCho/GPEtn/GPIns/GPSer"},
{340.0f,	Float.MAX_VALUE,	0.0f,		430.0f,				"Sphingolipids", 		-17.0027f,				"[(M+H)-18]",	"Cer"}, 
{0.0f,		Float.MAX_VALUE, 	330.0f, 	420.0f,				"Sphingolipids", 		1.007825f,				"[M+H]",		"SM"},  
{550.0f, 	Float.MAX_VALUE, 	0.0f, 		410.0f,				"Glycerophospholipi*", 	-96.9691f,				"[M-97]",		"GPA"},
{550.0f, 	Float.MAX_VALUE, 	0.0f,		410.0f,				"Glycerophospholipi*",	-171.0059f,				"[M-171]",		"GPGro"},		
{350.0f,	Float.MAX_VALUE, 	0.0f,		410.0f,				"Glycerolipids",		18.0344f,				"[M+18]",		"DAG"},
{0.0f,		Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerolipids",		18.0344f,				"[M+18]",		"TAG"},
{550.0f,	Float.MAX_VALUE, 	350.0f,		Float.MAX_VALUE,	"Sterollipids",			18.0344f,				"[M+18]",		"ChoE"}, 
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	3*22.98977f-2*1.007825f,	"[M-2H+3Na]+",	"CL"},
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	2*22.98977f-1.007825f,	"[M-H+2Na]+",	"CL"}, 
{1000.0f,	Float.MAX_VALUE, 	410.0f,		Float.MAX_VALUE,	"Glycerophospholipi*",	22.98977f,				"[M+Na]+",		"CL"}
};

	private static Object[] noMatchQuery = 
{0.0f,		0.0f,				0.0f,		0.0f,				"(.*)", 				1.007825f,				"AUTO",			"NOIDEA"};

	
	/*
	 * Postprocessing table 
	 */
	
	private final int ivtColMinRT = 0;
	private final int ivtColMaxRT = 1;
	private final int ivtColName =  2;
	
	private Object[][] identityValidationTable = {
{0.0f,		300.0f,				"Lyso"},
{0.0f,		400.0f,				"DAG"},
{410.0f,	Float.MAX_VALUE,	"TAG"},
{0.0f,		300.0f,				"MAG"},
{200.0f,	380.0f,				"GP"},
{0.0f,		430.0f,				"Cer"},
{0.0f,		420.0f,				"SM"},
{410.0f,	Float.MAX_VALUE,	"ChoE"}
	};
	
	private QBIXLipidDBSearchParameters parameters;

	QBIXLipidDBUtils(QBIXLipidDBSearchParameters parameters) {
		this.parameters = parameters;
	}

	/**
	 * Create and return queries corresponding to a single peak list row
	 */
	QBIXLipidDBQuery[] createQueries(PeakListRow peakListRow) {

		float mz = peakListRow.getAverageMZ();
		float rt = peakListRow.getAverageRT();

		ArrayList<QBIXLipidDBQuery> queries = new ArrayList<QBIXLipidDBQuery>();

		if (mz <= 0)
			return new QBIXLipidDBQuery[0];
		if (rt < 0)
			return new QBIXLipidDBQuery[0];

		float tolerancePPM = (Float) parameters
				.getParameterValue(QBIXLipidDBSearchParameters.MZTolerance);
		float resolution = (Float) parameters
				.getParameterValue(QBIXLipidDBSearchParameters.MassResolution);

		for (int queryNumber = 0; queryNumber < lipidClassTable.length; queryNumber++) {
			if ((mz > (Float) lipidClassTable[queryNumber][lctColMinMZ])
					&& (mz < (Float) lipidClassTable[queryNumber][lctColMaxMZ])
					&& (rt > (Float) lipidClassTable[queryNumber][lctColMinRT])
					&& (rt < (Float) lipidClassTable[queryNumber][lctColMaxRT])) {

				QBIXLipidDBQuery newQueryData = new QBIXLipidDBQuery(
						(String) lipidClassTable[queryNumber][lctColName], mz,
						rt, tolerancePPM,
						(String) lipidClassTable[queryNumber][lctColAdduct],
						(Float) lipidClassTable[queryNumber][lctColAdd],
						resolution,
						(String) lipidClassTable[queryNumber][lctColExpected]);

				queries.add(newQueryData);

			}

		}

		if (queries.isEmpty()) {

			QBIXLipidDBQuery newQueryData = new QBIXLipidDBQuery(
					(String) noMatchQuery[lctColName], mz, rt, tolerancePPM,
					(String) noMatchQuery[lctColAdduct],
					(Float) noMatchQuery[lctColAdd], resolution,
					(String) noMatchQuery[lctColExpected]);

			queries.add(newQueryData);

		}

		return queries.toArray(new QBIXLipidDBQuery[0]);

	}
	
	/**
	 * Validate if identity is possible for the query
	 */
	boolean validateIdentityForQuery(QBIXLipidDBQuery query, CompoundIdentity identity) {
		
		for (int tableRow=0; tableRow<identityValidationTable.length; tableRow++) {
			if (	(query.getRT()>(Float)identityValidationTable[tableRow][ivtColMinRT]) &&
					(query.getRT()<(Float)identityValidationTable[tableRow][ivtColMaxRT]) &&
					(identity.getCompoundName().contains( (String)identityValidationTable[tableRow][ivtColName])) ) {
				return true;
			}
		}
		return false;
		
	}
	

}
