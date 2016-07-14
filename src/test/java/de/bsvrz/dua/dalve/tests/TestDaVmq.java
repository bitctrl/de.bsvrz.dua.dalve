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
import de.bsvrz.dav.daf.main.config.ObjectTimeSpecification;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.tests.DuaLayout;
import org.junit.Before;
import org.junit.Test;

import java.util.Collections;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class TestDaVmq extends DaLveTestBase {

	private DataDescription ddOut;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		for(SystemObject obj : _dataModel.getObjects(
				Collections.singletonList(_dataModel.getConfigurationArea("kb.duaTestFs")),
				Collections.singletonList(_dataModel.getType("typ.messQuerschnittAllgemein")),
				ObjectTimeSpecification.valid())) {
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
			publishPrognoseParamsMq(obj);

		}	
	
		AttributeGroup atgOutput = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitMq");
		Aspect aspOutput = _dataModel.getAspect("asp.analyse");
		ddOut = new DataDescription(atgOutput, aspOutput);

		Thread.sleep(1000);
	}

	@Test
	public void testDua81p1a() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.vor1").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.mitte", "mq.aus").toArray(new SystemObject[2]);
		startTestCase("DUA81-1a.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	
	@Test
	public void testDua81p1b() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.vor2").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.nach", "mq.aus", "mq.ein").toArray(new SystemObject[3]);
		startTestCase("DUA81-1b.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	@Test
	public void testDua81p2a() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.mitte1").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.vor", "mq.aus").toArray(new SystemObject[2]);
		startTestCase("DUA81-2a.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	
	@Test
	public void testDua81p2b() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.mitte2").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.nach", "mq.ein").toArray(new SystemObject[2]);
		startTestCase("DUA81-2b.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	@Test
	public void testDua81p3a() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.nach1").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.mitte", "mq.ein").toArray(new SystemObject[2]);
		startTestCase("DUA81-3a.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	
	@Test
	public void testDua81p3b() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.nach2").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.vor", "mq.ein", "mq.aus").toArray(new SystemObject[3]);
		startTestCase("DUA81-3b.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}
	
	@Test
	public void testDua82p1() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.allgemein.1").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2").toArray(new SystemObject[2]);
		startTestCase("DUA82-1.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}	
	@Test
	public void testDua82p2() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.allgemein.2").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2").toArray(new SystemObject[2]);
		startTestCase("DUA82-2.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}	
	@Test
	public void testDua82p3() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.allgemein.3").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2", "mq.3").toArray(new SystemObject[3]);
		startTestCase("DUA82-3.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}	
	@Test
	public void testDua83p1() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.vlage.1").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2").toArray(new SystemObject[2]);
		startTestCase("DUA83-1.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}	
	@Test
	public void testDua83p2() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.vlage.2").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2").toArray(new SystemObject[2]);
		startTestCase("DUA83-2.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}	
	@Test
	public void testDua83p3() throws Exception {
		SystemObject[] vmq = _dataModel.getObjects("vmq.vlage.3").toArray(new SystemObject[1]);
		SystemObject[] mqs = _dataModel.getObjects("mq.1", "mq.2", "mq.3").toArray(new SystemObject[3]);
		startTestCase("DUA83-3.csv", mqs, vmq, ddOut, ddOut, new DuaLayout());
	}

}
