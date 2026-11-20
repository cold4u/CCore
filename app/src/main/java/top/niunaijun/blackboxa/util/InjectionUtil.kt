package com.ccorea.util

import com.ccorea.data.AppsRepository
import com.ccorea.data.FakeLocationRepository
import com.ccorea.data.GmsRepository
import com.ccorea.data.XpRepository
import com.ccorea.view.apps.AppsFactory
import com.ccorea.view.fake.FakeLocationFactory
import com.ccorea.view.gms.GmsFactory
import com.ccorea.view.list.ListFactory
import com.ccorea.view.xp.XpFactory

/**
 *
 * @Description:
 * @Author: wukaicheng
 * @CreateDate: 2021/4/29 22:38
 */
object InjectionUtil {

    private val appsRepository = AppsRepository()

    private val xpRepository = XpRepository()

    private val gmsRepository = GmsRepository()

    private val fakeLocationRepository = FakeLocationRepository()

    fun getAppsFactory() : AppsFactory {
        return AppsFactory(appsRepository)
    }

    fun getListFactory(): ListFactory {
        return ListFactory(appsRepository)
    }

    fun getXpFactory():XpFactory{
        return XpFactory(xpRepository)
    }

    fun getGmsFactory():GmsFactory{
        return GmsFactory(gmsRepository)
    }

    fun getFakeLocationFactory():FakeLocationFactory{
        return FakeLocationFactory(fakeLocationRepository)
    }
}