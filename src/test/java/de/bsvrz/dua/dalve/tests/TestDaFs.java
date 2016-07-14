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

import com.google.common.collect.ImmutableSet;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.ResultData;
import de.bsvrz.dav.daf.main.SendSubscriptionNotConfirmed;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.tests.ColumnLayout;
import de.bsvrz.dua.tests.ComplexDataDescription;
import de.bsvrz.dua.tests.DuaLayout;
import de.bsvrz.dua.tests.DuaStatusLayout;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class TestDaFs extends DaLveTestBase {

	private DataDescription _ddIn;
	private DataDescription _ddOut;
	private SystemObject[] _testFs;
	private Aspect[] prognoseAspects;
	private AttributeGroup atgPrognose1;
	private AttributeGroup atgPrognose2;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		_testFs = new SystemObject[]{_dataModel.getObject("fs.mq.1.hfs")};

		for(SystemObject obj : _testFs) {
			fakeParamApp.publishParam(obj.getPid(), "atg.verkehrsDatenKurzZeitAnalyseFs",
			                          "{" +
					                          "kKfz:{Grenz:'48',Max:'68'}," +
					                          "kLkw:{Grenz:'28',Max:'38'}," +
					                          "kPkw:{Grenz:'48',Max:'68'}," +
					                          "kB:{Grenz:'58',Max:'77'}," +
					                          "fl:{k1:'2,2',k2:'0,02'}" +
					                          "}"
			);

			publishPrognoseParamsFs(obj);

		}

		AttributeGroup atgInput = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitIntervall");
		AttributeGroup atgOutput = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitFs");
		atgPrognose1 = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitTrendExtraPolationFs");
		atgPrognose2 = _dataModel.getAttributeGroup("atg.verkehrsDatenKurzZeitGeglättetFs");
		Aspect aspPrognoseFlink = _dataModel.getAspect("asp.prognoseFlink");
		Aspect aspPrognoseNormal = _dataModel.getAspect("asp.prognoseNormal");
		Aspect aspPrognoseTraege = _dataModel.getAspect("asp.prognoseTräge");
		prognoseAspects = new Aspect[]{aspPrognoseFlink, aspPrognoseNormal, aspPrognoseTraege};
		Aspect aspInput = _dataModel.getAspect("asp.messWertErsetzung");
		Aspect aspOutput = _dataModel.getAspect("asp.analyse");
		_ddIn = new DataDescription(atgInput, aspInput);
		_ddOut = new DataDescription(atgOutput, aspOutput);

		Thread.sleep(1000);
	}

	@Test
	public void testDua25() throws Exception {
		startTestCase("DUA25.csv", _testFs, _testFs, _ddIn, _ddOut, new DuaLayout(){
			@Override
			public boolean groupingEnabled() {
				return false;
			}
		});
	}
	
	@Test
	public void testDua25Status() throws Exception {
		startTestCase("DUA25Status.csv", _testFs, _testFs, _ddIn, _ddOut, new DuaStatusLayout(){
			@Override
			public boolean groupingEnabled() {
				return false;
			}
		});
	}
	
	@Test
	public void testDua3637() throws Exception {
		ColumnLayout columnLayout = new ColumnLayout() {
			@Override
			public boolean groupingEnabled() {
				return false;
			}
			
			@Override
			public Collection<String> getIgnored() {
				return ImmutableSet.of("aLkwP", "kKfzP", "kLkwP", "kPkwP", "qBP", "kBP", "aLkwG", "kKfzG", "kLkwG", "kPkwG", "qBG", "kBG");
			}

			@Override
			public int getColumnCount(final boolean in) {
				return 1;
			}

			@Override
			public void setValues(final SystemObject testObject, final Data item, final List<String> row, final int realCol, final String type, final boolean in) {
				item.getTextValue("Wert").setText(row.get(realCol));
			}

			@Override
			public List<Data> getSubDatas(final String type, final Data data) {
				ArrayList<Data> result = new ArrayList<>();
				result.addAll(super.getSubDatas(type, data));
				result.addAll(super.getSubDatas(type.replaceAll("Kfz", "Pkw"), data));
				result.addAll(super.getSubDatas(type.replaceAll("Kfz", "Lkw"), data));
				return result;
			}
		};

		for(Aspect prognoseAspect : prognoseAspects) {
//			System.out.println("prognoseAspect = " + prognoseAspect);
			startTestCase("DUA36.37.csv", _testFs, _testFs, _ddOut, new ComplexDataDescription(Arrays.asList(atgPrognose1, atgPrognose2), prognoseAspect), columnLayout);
			return;
		}
	}
	

	@Override
	public void sendData(final ResultData... resultDatas) throws SendSubscriptionNotConfirmed {
		if(Objects.equals(resultDatas[0].getDataDescription(), _ddIn)) {
			_datenaufbereitungLVE.update(resultDatas);
		}
		else {
			super.sendData(resultDatas);
		}
	}
}
