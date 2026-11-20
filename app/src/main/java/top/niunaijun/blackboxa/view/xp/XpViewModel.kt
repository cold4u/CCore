package com.ccorea.view.xp

import androidx.lifecycle.MutableLiveData
import com.ccorea.bean.XpModuleInfo
import com.ccorea.data.XpRepository
import com.ccorea.view.base.BaseViewModel

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/5/2 20:55
 */
class XpViewModel(private val repo:XpRepository):BaseViewModel() {

    val appsLiveData = MutableLiveData<List<XpModuleInfo>>()

    val resultLiveData = MutableLiveData<String>()

    fun getInstalledModule() {
        launchOnUI {
            repo.getInstallModules(appsLiveData)
        }
    }

    fun installModule(source:String) {
        launchOnUI {
            repo.installModule(source,resultLiveData)
        }
    }


    fun unInstallModule(packageName: String){
        launchOnUI {
            repo.unInstallModule(packageName,resultLiveData)
        }
    }
}