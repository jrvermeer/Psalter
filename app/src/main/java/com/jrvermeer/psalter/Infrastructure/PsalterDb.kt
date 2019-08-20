package com.jrvermeer.psalter.Infrastructure

import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteQueryBuilder
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

import com.jrvermeer.psalter.R
import com.jrvermeer.psalter.Core.Contracts.IPsalterRepository
import com.jrvermeer.psalter.Core.Models.Psalter
import com.jrvermeer.psalter.Core.Models.SqLiteQuery
import com.readystatesoftware.sqliteasset.SQLiteAssetHelper

import java.util.ArrayList

/**
 * Created by Jonathan on 3/27/2017.
 */
// SQLiteAssetHelper: https://github.com/jgilfelt/android-sqlite-asset-helper
class PsalterDb(private val context: Context) : SQLiteAssetHelper(context, DATABASE_NAME, null, DATABASE_VERSION), IPsalterRepository {
    private val db: SQLiteDatabase

    init {
        setForcedUpgrade(DATABASE_VERSION)
        db = readableDatabase
    }

    companion object {
        private const val DATABASE_VERSION = 25
        private const val DATABASE_NAME = "psalter.sqlite"
        private const val TABLE_NAME = "psalter"
    }

    override fun getCount(): Int {
        return try {
            DatabaseUtils.queryNumEntries(db, TABLE_NAME).toInt()
        } catch (ex: Exception) { 0 }
    }

    override fun getIndex(index: Int): Psalter? {
        val results = queryPsalter("_id = $index", null, "1")
        return if (results.isEmpty()) null else results[0]
    }

    override fun getPsalter(number: Int): Psalter? {
        val results = queryPsalter("number = $number", null, "1")
        return if (results.isEmpty()) null else results[0]
    }

    override fun getRandom(): Psalter {
        val results = queryPsalter("_id IN (SELECT _id FROM $TABLE_NAME ORDER BY RANDOM() LIMIT 1)", null, null)
        return results[0]
    }

    override fun getPsalm(psalmNumber: Int): Array<Psalter> {
        return queryPsalter("psalm = $psalmNumber", null, null)
    }

    override fun searchPsalter(searchText: String): Array<Psalter> {
        var c: Cursor? = null
        val hits = ArrayList<Psalter>()
        try {
            val lyrics = lyricsReplacePunctuation()
            lyrics.addParameter("%$searchText%")

            c = db.rawQuery("select _id, number, psalm, " + lyrics.queryText + " l from psalter where l like ?", lyrics.parameters)
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
            val columns = arrayOf("_id", "number", "psalm", "title", "lyrics", "numverses", "heading", "AudioFileName", "ScoreFileName", "NumVersesInsideStaff")
            qb.tables = TABLE_NAME
            c = qb.query(db, columns, where, parms, null, null, null, limit)
            while (c!!.moveToNext()) {
                val p = Psalter()

                p.id = c.getInt(0)
                p.number = c.getInt(1)
                p.psalm = c.getInt(2)
                p.title = c.getString(3)
                p.lyrics = c.getString(4)
                p.numverses = c.getInt(5)
                p.heading = c.getString(6)
                p.audioFileName = c.getString(7)
                p.scoreFileName = c.getString(8)
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
        query.queryText = "lyrics"
        for (ignore in context.resources.getStringArray(R.array.search_ignore_strings)) {
            query.queryText = "replace(" + query.queryText + ", ?, '')"
            query.addParameter(ignore)
        }
        return query
    }

    override fun getAudioDescriptor(psalter: Psalter): AssetFileDescriptor? {
        return try {
            context.assets.openFd("Audio/" + psalter.audioFileName)
        } catch (ex: Exception) {
            Logger.e("Error getting audio " + psalter.title, ex)
            null
        }

    }

    override fun getScore(psalter: Psalter): BitmapDrawable? {
        return try {
            Drawable.createFromStream(context.assets.open("Score/" + psalter.scoreFileName), null) as BitmapDrawable
        } catch (ex: Exception) {
            Logger.e("Error getting score " + psalter.title, ex)
            null
        }

    }


}
