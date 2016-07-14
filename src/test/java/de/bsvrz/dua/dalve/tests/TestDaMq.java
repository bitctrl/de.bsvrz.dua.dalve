/*
 * Copyright 2016 by Kappich Systemberatung Aachen
 * 
 * This file is part of de.bsvrz.dua.dalve.tests.
 * 
 * de.bsvrz.dua.dalve.tests is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * de.bsvrz.dua.dalve.tests is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with de.bsvrz.dua.dalve.tests.  If not, see <http://www.gnu.org/licenses/>.

 * Contact Information:
 * Kappich Systemberatung
 * Martin-Luther-Straße 14
 * 52062 Aachen, Germany
 * phone: +49 241 4090 436 
 * mail: <info@kappich.de>
 */

package de.bsvrz.dua.dalve.tests;

import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.tests.DuaLayout;
import org.junit.Before;
import org.junit.Test;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class TestDaMq extends DaLveTestBase {

	private DataDescription ddIn;
	private DataDescription ddOut;
	private SystemObject[] testFs3;
	private SystemObject[] testMq3;
	private SystemObject[] testFs1;
	private SystemObject[] testMq1;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		testFs3 = new SystemObject[]{
				_dataModel.getObject("fs.mq.3.hfs"),
				_dataModel.getObject("fs.mq.3.1üfs"),
				_dataModel.getObject("fs.mq.3.2üfs")
		};

		testMq3 = new SystemObject[]{
				_dataModel.getObject("mq.3")
		};	
		testFs1 = new SystemObject[]{
				_dataModel.getObject("fs.mq.1.hfs")
		};

		testMq1 = new SystemObject[]{
				_dataModel.getObject("mq.1")
		};

		for(SystemObject obj : testMq1) {
			fakeParamApp.publishParam(obj.getPid(), "atg.verkehrsDatenKurzZeitAnalyseMq",
			                          "{" +
					                          "KKfz:{Grenz:'48',Max:'68'}," +
					                          "KLkw:{Grenz:'28',Max:'38'}," +
					                          "KPkw:{Grenz:'48',Max:'68'}," +
					                          "KB:{Grenz:'58',Max:'77'}," +
					                          "fl:{k1:'2,2',k2:'0,02'}," +
					                          "wichtung:['40','60']" +
					                          "}"
			);

		}	
		for(SystemObject obj : testMq3) {
			fakeParamApp.publishParam(obj.getPid(), "atg.verkehrsDatenKurzZeitAnalyseMq",
			                          "{" +
					                          "KKfz:{Grenz:'48',Max:'68'}," +
					                          "KLkw:{Grenz:'28',Max:'38'}," +
					                          "KPkw:{Grenz:'48',Max:'68'}," +
					                          "KB:{Grenz:'58',Max:'77'}," +
					                          "fl:{k1:'2,2',k2:'0,02'}," +
					                          "wichtung:['40','60']" +
					                          "}"
			);

		}

		AttributeGroup atgInput = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitFs");
		AttributeGroup atgOutput = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitMq");
		Aspect aspInput = _dataModel.getAspect("asp.analyse");
		Aspect aspOutput = _dataModel.getAspect("asp.analyse");
		ddIn = new DataDescription(atgInput, aspInput);
		ddOut = new DataDescription(atgOutput, aspOutput);

		Thread.sleep(1000);
	}

	@Test
	public void testDua26p3a() throws Exception {
		startTestCase("DUA26-3a.csv", testFs3, testMq3, ddIn, ddOut, new DuaLayout());
	}
	@Test
	public void testDua26p1a() throws Exception {
		startTestCase("DUA26-1a.csv", testFs1, testMq1, ddIn, ddOut, new DuaLayout());
	}
}
