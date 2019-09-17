package com.jrvermeer.psalter.infrastructure

import android.content.Context
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.helpers.DownloadHelper
import com.jrvermeer.psalter.models.Psalter
import com.jrvermeer.psalter.models.SqLiteQuery
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.util.*

/**
 * Created by Jonathan on 3/27/2017.
 */
// SQLiteAssetHelper: https://github.com/jgilfelt/android-sqlite-asset-helper
class PsalterDb(private val context: Context,
                private val scope: CoroutineScope) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    val downloader = DownloadHelper(context)
    private val db: SQLiteDatabase
    private var nextRandom : Psalter? = null

    init {
        setForcedUpgrade(DATABASE_VERSION)
        db = readableDatabase
        scope.launch { loadNextRandom() }
    }

    companion object {
        private const val DATABASE_VERSION = 29
        private const val DATABASE_NAME = "psalter.sqlite"
        private const val TABLE_NAME = "psalter"
    }

    fun getCount(): Int {
        return try {
            DatabaseUtils.queryNumEntries(db, TABLE_NAME).toInt()
        } catch (ex: Exception) { 0 }
    }

    fun getIndex(index: Int): Psalter? {
        val results = queryPsalter("_id = $index", null, "1")
        return if (results.isEmpty()) null else results[0]
    }

    fun getPsalter(number: Int): Psalter? {
        val results = queryPsalter("number = $number", null, "1")
        return if (results.isEmpty()) null else results[0]
    }

    private fun CoroutineScope.loadNextRandom() {
        nextRandom = queryRandom()
        launch { nextRandom!!.loadAudio(downloader) }
        launch { nextRandom!!.loadScore(downloader) }
    }
    fun getRandom(): Psalter {
        val rtn = nextRandom
        scope.launch { loadNextRandom() }
        return rtn!!
    }

    private fun queryRandom(): Psalter {
        val results = queryPsalter("_id IN (SELECT _id FROM $TABLE_NAME ORDER BY RANDOM() LIMIT 1)", null, null)
        return results[0]
    }

    fun getPsalm(psalmNumber: Int): Array<Psalter> {
        return queryPsalter("psalm = $psalmNumber", null, null)
    }

    fun searchPsalter(searchText: String): Array<Psalter> {
        var c: Cursor? = null
        val hits = ArrayList<Psalter>()
        try {
            val lyrics = lyricsReplacePunctuation()
            lyrics.parameters.add("%$searchText%")

            c = db.rawQuery("select _id, number, psalm, ${lyrics.text} l from psalter where l like ?", lyrics.parameters.toTypedArray())
            while (c!!.moveToNext()) {
                val p = Psalter()
                p.id = c.getInt(0)
                p.number = c.getInt(1)
                p.psalm = c.getInt(2)
                p.lyrics = c.getString(3)
                hits.add(p)
            }
        } catch (ex: Exception) {
            Logger.e("Error searching psalter: $searchText", ex)
        } finally {
            c?.close()
        }
        return hits.toTypedArray()
    }

    private fun queryPsalter(where: String, parms: Array<String>?, limit: String?): Array<Psalter> {
        var c: Cursor? = null
        val hits = ArrayList<Psalter>()
        try {
            val qb = SQLiteQueryBuilder()
            val columns = arrayOf("_id", "number", "psalm", "title", "lyrics", "numverses", "heading", "audioFileName", "scoreFileName", "NumVersesInsideStaff")
            qb.tables = TABLE_NAME
            c = qb.query(db, columns, where, parms, null, null, null, limit)
            while (c.moveToNext()) {
                val p = Psalter()

                p.id = c.getInt(0)
                p.number = c.getInt(1)
                p.psalm = c.getInt(2)
                p.title = c.getString(3)
                p.lyrics = c.getString(4)
                p.numverses = c.getInt(5)
                p.heading = c.getString(6)
                p.audioPath = c.getString(7)
                p.scorePath = c.getString(8)
                p.numVersesInsideStaff = c.getInt(9)
                hits.add(p)
            }
        } catch (ex: Exception) {
            Logger.e("Error querying psalter: $where", ex)
        } finally {
            c?.close()
        }
        return hits.toTypedArray()
    }

    private fun lyricsReplacePunctuation(): SqLiteQuery {
        val query = SqLiteQuery()
        query.text = "lyrics"
        for (ignore in context.resources.getStringArray(R.array.search_ignore_strings)) {
            query.text = "replace(${query.text}, ?, '')"
            query.parameters.add(ignore)
        }
        return query
    }
}
