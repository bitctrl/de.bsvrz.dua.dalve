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

import de.bsvrz.dav.daf.main.ClientSenderInterface;
import de.bsvrz.dav.daf.main.Data;
import de.bsvrz.dav.daf.main.DataDescription;
import de.bsvrz.dav.daf.main.SenderRole;
import de.bsvrz.dav.daf.main.config.Aspect;
import de.bsvrz.dav.daf.main.config.AttributeGroup;
import de.bsvrz.dav.daf.main.config.SystemObject;
import de.bsvrz.dua.tests.DuADataIdentification;
import de.bsvrz.dua.tests.DuaLayout;
import de.bsvrz.sys.funclib.bitctrl.dua.DUAKonstanten;
import de.bsvrz.sys.funclib.kappich.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * TBD Dokumentation
 *
 * @author Kappich Systemberatung
 */
public class TestDaStoerfall extends DaLveTestBase {

	private DataDescription _ddIn;
	private SystemObject[] _testFs;
	private Aspect _aspMarz;
	private Aspect _aspNRW;
	private Aspect _aspRds;
	private Aspect _aspFd;
	private AttributeGroup _atgOutput;
	private SystemObject _testMq1;
	private SystemObject _testMq2;
	private SystemObject _testMq3;
	private List<DuADataIdentification> _ddInlist;
	private ClientSenderInterface _sender;

	@Before
	public void setUp() throws Exception {
		super.setUp();
		_testFs = new SystemObject[]{
				_dataModel.getObject("fs.mq.1.hfs"),
				_dataModel.getObject("fs.mq.2.hfs"),
				_dataModel.getObject("fs.mq.2.1üfs"),
				_dataModel.getObject("fs.mq.3.hfs"),
				_dataModel.getObject("fs.mq.3.1üfs"),
				_dataModel.getObject("fs.mq.3.2üfs"),
		};

		_testMq1 = _dataModel.getObject("mq.1");
		_testMq2 = _dataModel.getObject("mq.2");
		_testMq3 = _dataModel.getObject("mq.3");

		publishPrognoseParamsMq(_testMq1);
		publishPrognoseParamsMq(_testMq2);
		publishPrognoseParamsMq(_testMq3);
		publishStoerfallParam(_testMq1);
		publishStoerfallParam(_testMq2);
		publishStoerfallParam(_testMq3);

		for(SystemObject obj : _testFs) {
			publishStoerfallParam(obj);

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
		_atgOutput = _dataModel.getAttributeGroup("atg.störfallZustand");
		Aspect aspInput = _dataModel.getAspect("asp.messWertErsetzung");
		_aspMarz = _dataModel.getAspect("asp.störfallVerfahrenMARZ");
		_aspNRW = _dataModel.getAspect("asp.störfallVerfahrenNRW");
		_aspRds = _dataModel.getAspect("asp.störfallVerfahrenRDS");
		_aspFd = _dataModel.getAspect("asp.störfallVerfahrenFD");
		_ddIn = new DataDescription(atgInput, aspInput);

		Thread.sleep(1000);

		_sender = new ClientSenderInterface() {
			@Override
			public void dataRequest(final SystemObject object, final DataDescription dataDescription, final byte state) {

			}

			@Override
			public boolean isRequestSupported(final SystemObject object, final DataDescription dataDescription) {
				return false;
			}
		};
		_connection.subscribeSender(_sender, _testFs, _ddIn, SenderRole.source());

		_ddInlist = Arrays.asList(_testFs).stream().map(systemObject -> new DuADataIdentification(systemObject, _ddIn)).collect(Collectors.toList());
	}

	@Override
	@After
	public void tearDown() throws Exception {
		super.tearDown();
	}

	@NotNull
	protected List<DuADataIdentification> makeOutDD(final SystemObject object, final Aspect aspect) {
		return Collections.singletonList(new DuADataIdentification(object, new DataDescription(_atgOutput, aspect)));
	}

	protected void publishStoerfallParam(final SystemObject obj) {
		fakeParamApp.publishParam(obj.getPid(), "atg.verkehrsLageVerfahren2",
		                          "{" +
				                          "v1:'71'," +
				                          "v2:'97'," +
				                          "k1:'5'," +
				                          "k2:'15'," +
				                          "k3:'25'," +
				                          "kT:'10'" +
				                          "}"
		);	
		if(obj.isOfType(DUAKonstanten.TYP_MQ)) {
			fakeParamApp.publishParam(obj.getPid(), "atg.verkehrsLageVerfahren3",
			                          "{" +
					                          "v1:'71'," +
					                          "v2:'97'," +
					                          "k1:'5'," +
					                          "k2:'15'," +
					                          "k3:'25'," +
					                          "kT:'10'," +
					                          "VST5Hysterese:'7'," +
					                          "VST6Hysterese:'7'" +
					                          "}"
			);
		}
	}

	@Test
	public void testDuaStoerfallNRWFs() throws Exception {
		startTestCase("DUAStNRWFs.csv", _ddInlist, makeOutDD(_testFs[0], _aspNRW), new DuaStLayout());
	}
	
	@Test
	public void testDuaStoerfallNRWMq1() throws Exception {
		startTestCase("DUAStNRWMq1.csv", _ddInlist, makeOutDD(_testMq1, _aspNRW), new DuaStLayout());
	}
	
	@Test
	public void testDuaStoerfallNRWMq2() throws Exception {
		startTestCase("DUAStNRWMq2.csv", _ddInlist, makeOutDD(_testMq2, _aspNRW), new DuaStLayout());
	}
	
	@Test
	public void testDuaStoerfallNRWMq3() throws Exception {
		startTestCase("DUAStNRWMq3.csv", _ddInlist, makeOutDD(_testMq3, _aspNRW), new DuaStLayout());
	}
	
	@Test
	public void testDuaStoerfallRDS() throws Exception {
		startTestCase("DUAStRDS.csv", _ddInlist, makeOutDD(_testMq3, _aspRds), new DuaStLayout());
	}

	private class DuaStLayout extends DuaLayout {

		@Override
		public int getColumnCount(final boolean in) {
			return in ? 4 : 1;
		}

		@Override
		public void setValues(final SystemObject testObject, Data item, final List<String> row, final int realCol, final String type, final boolean in) {
			if(in){
				super.setValues(testObject, item, row, realCol, type, in);
			}
			else {
				if(item.isList()) item = item.getItem("Index");
				item.asTextValue().setText(row.get(realCol));
			}
		}
	}

	//	@Override
//	public void sendData(final ResultData... resultDatas) throws SendSubscriptionNotConfirmed {
//		for(ResultData resultData : resultDatas) {
//			try {
//				DavTestUtil.sendData(resultData, SenderRole.source());
//			}
//			catch(OneSubscriptionPerSendData oneSubscriptionPerSendData) {
//				oneSubscriptionPerSendData.printStackTrace();
//			}
//		}
//	}
}
