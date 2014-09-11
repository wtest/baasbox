/*
 * Copyright (c) 2014.
 *
 * BaasBox - info-at-baasbox.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.baasbox.db;

import play.Logger;

import com.baasbox.configuration.Push;
import com.baasbox.configuration.index.IndexPushConfiguration;
import com.baasbox.dao.RoleDao;
import com.baasbox.enumerations.DefaultRoles;
import com.baasbox.exception.IndexNotFoundException;
import com.baasbox.service.user.RoleService;
import com.orientechnologies.orient.core.db.record.ODatabaseRecordTx;
import com.orientechnologies.orient.core.metadata.security.ORole;

public class Evolution_0_9_0 implements IEvolution {
	private String version="0.9.0";
	
	public Evolution_0_9_0() {}

	@Override
	public String getFinalVersion() {
		return version;
	}

	@Override
	public void evolve(ODatabaseRecordTx db) {
		Logger.info ("Applying evolutions to evolve to the " + version + " level");
		try{
			registeredRoleInheritsFromAnonymousRole(db);
			updateDefaultTimeFormat(db);
			multiPushProfileSettings(db);
		}catch (Throwable e){
			Logger.error("Error applying evolution to " + version + " level!!" ,e);
			throw new RuntimeException(e);
		}
		Logger.info ("DB now is on " + version + " level");
	}
	
	//issue #195 Registered users should have access to anonymous resources
	private void registeredRoleInheritsFromAnonymousRole(ODatabaseRecordTx db) {
		Logger.info("...updating registered role");
		ORole regRole = RoleDao.getRole(DefaultRoles.REGISTERED_USER.toString());
		regRole.getDocument().field(RoleDao.FIELD_INHERITED,DefaultRoles.ANONYMOUS_USER.getORole().getDocument().getRecord());
		regRole.save();
		Logger.info("...done");
	}
	
	private void updateDefaultTimeFormat(ODatabaseRecordTx db) {
			DbHelper.execMultiLineCommands(db,true,"alter database DATETIMEFORMAT yyyy-MM-dd'T'HH:mm:ss.SSSZ");
	}
	
	/***
	 * creates new records for new push settings and migrates the old ones into the profile n.1
	 * @param db
	 */
	private void multiPushProfileSettings(ODatabaseRecordTx db) {
		IndexPushConfiguration idx;
		try {
			idx = new IndexPushConfiguration();
		} catch (IndexNotFoundException e) {
			throw new RuntimeException(e);
		}
		
		//load the old settings
		String sandbox = (idx.get("push.sandbox.enable")).toString();
		String appleTimeout= (idx.get("push.apple.timeout")).toString();
		
		String sandboxAndroidApiKey= (idx.get("sandbox.android.api.key")).toString();
		String sandBoxIosCertificatePassword = (idx.get("sandbox.ios.certificate.password")).toString();
		
		String prodAndroidApiKey= (idx.get("production.android.api.key")).toString();
		String prodBoxIosCertificatePassword = (idx.get("production.ios.certificate.password")).toString();
		
		//Houston we have a problem. Here we have to handle the iOS certicates that are files!
		//String sandBoxIosCertificate = (idx.get("sandbox.ios.certificate")).toString();
		//String prodBoxIosCertificate = (idx.get("production.ios.certificate")).toString();
		
		try{
			//set the new profile1 settings
			Push.PROFILE1_PUSH_PROFILE_ENABLE.setValue(true);
			Push.PROFILE1_PUSH_SANDBOX_ENABLE.setValue(sandbox);
			Push.PROFILE1_PRODUCTION_ANDROID_API_KEY.setValue(prodAndroidApiKey);
			//Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE
			Push.PROFILE1_PRODUCTION_IOS_CERTIFICATE_PASSWORD.setValue(prodBoxIosCertificatePassword);
			Push.PROFILE1_PUSH_APPLE_TIMEOUT.setValue(appleTimeout);
			Push.PROFILE1_SANDBOX_ANDROID_API_KEY.setValue(sandboxAndroidApiKey);
			//Push.PROFILE1_SANDBOX_IOS_CERTIFICATE
			Push.PROFILE1_SANDBOX_IOS_CERTIFICATE_PASSWORD.setValue(sandBoxIosCertificatePassword);
			
			//disable other profiles
			Push.PROFILE2_PUSH_PROFILE_ENABLE.setValue(false);
			Push.PROFILE3_PUSH_PROFILE_ENABLE.setValue(false);
		}catch (Exception e){
			throw new RuntimeException(e);
		}	
	}
    
}