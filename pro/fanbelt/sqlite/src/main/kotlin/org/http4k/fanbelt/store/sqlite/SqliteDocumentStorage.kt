/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.store.sqlite

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import com.zaxxer.hikari.SQLExceptionOverride
import com.zaxxer.hikari.SQLExceptionOverride.Override.CONTINUE_EVICT
import com.zaxxer.hikari.SQLExceptionOverride.Override.MUST_EVICT
import org.http4k.fanbelt.store.DocumentStorage
import org.http4k.fanbelt.store.exposed.ExposedDocumentStorage
import org.http4k.format.AutoMarshallingJson
import org.jetbrains.exposed.v1.jdbc.Database
import org.sqlite.SQLiteConfig
import org.sqlite.SQLiteConfig.JournalMode.WAL
import org.sqlite.SQLiteConfig.TransactionMode.DEFERRED
import org.sqlite.SQLiteConfig.TransactionMode.IMMEDIATE
import org.sqlite.SQLiteDataSource
import org.sqlite.SQLiteErrorCode.SQLITE_BUSY
import org.sqlite.SQLiteException
import java.io.File
import java.sql.SQLException

fun SqliteDatabase(file: File, config: SQLiteConfig = writeSQLiteConfig, connections: Int = 4) =
    Database.connect(HikariDataSource(HikariConfig().apply {
        dataSource = SQLiteDataSource(config).apply { url = "jdbc:sqlite:${file.path}" }
        maximumPoolSize = connections
        exceptionOverride = EvictWhenBusy
    }))

private object EvictWhenBusy : SQLExceptionOverride {
    override fun adjudicate(e: SQLException) =
        if (e is SQLiteException && e.resultCode.code and 0xff == SQLITE_BUSY.code) MUST_EVICT else CONTINUE_EVICT
}

fun SqliteDocumentStorage(file: File, json: AutoMarshallingJson<*>, connections: Int = 4): DocumentStorage =
    ExposedDocumentStorage(SqliteDatabase(file, writeSQLiteConfig, connections), json, SqliteDocuments, readDb = SqliteDatabase(file, readSQLiteConfig, connections))

val writeSQLiteConfig = SQLiteConfig().apply {
    setJournalMode(WAL)
    transactionMode = IMMEDIATE
    setBusyTimeout(5000)
}

val readSQLiteConfig = SQLiteConfig().apply {
    setJournalMode(WAL)
    transactionMode = DEFERRED
    setBusyTimeout(5000)
}
