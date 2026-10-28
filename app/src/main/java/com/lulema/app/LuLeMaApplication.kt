package com.lulema.app

import android.app.Application
import androidx.room.Room
import com.lulema.app.data.*

class LuLeMaApplication: Application() {
 val database by lazy { Room.databaseBuilder(this,ArchiveDatabase::class.java,"archive.db").build() }
 val repository by lazy { ArchiveRepository(database) }
}
