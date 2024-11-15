package org.http4k.connect.amazon.dynamodb.mapper

import java.time.LocalDate
import java.util.UUID

internal data class Cat(
    val ownerId: UUID,
    val id: UUID,
    val name: String,
    val nickName: String = name,
    val born: LocalDate,
)

internal data class CatRef(
    val ownerId: UUID,
    val id: UUID,
    val name: String
)

internal fun Cat.ref() = CatRef(ownerId, id, name)

internal val owner1 = UUID.fromString("97f5acb1-7212-44f3-8ade-03cfa115c960")
internal val owner2 = UUID.fromString("5100dd9e-b28f-4a3b-8641-ff9d39d9bb08")

internal val toggles = Cat(
    ownerId = owner1,
    id = UUID.fromString("f23704a7-e560-4937-bfab-5d81ad0e16e5"),
    name = "Toggles",
    born = LocalDate.of(2004, 6, 4)
)

internal val smokie = Cat(
    ownerId = owner2,
    id = UUID.fromString("74555b4b-2248-4d19-8f67-6a5e871b3159"),
    name = "Smokie",
    born = LocalDate.of(2018, 7, 1)
)

internal val bandit = Cat(
    ownerId = owner2,
    id = UUID.fromString("c27d8f9d-ffac-45ce-889f-9024d9d0bf2d"),
    name = "Bandit",
    nickName = "Fatty",
    born = LocalDate.of(2018, 7, 1)
)

internal val kratos = Cat(
    ownerId = owner1,
    id = UUID.fromString("bf191fc3-b660-477b-8b9f-fc4a856f20af"),
    name = "Kratos",
    born = LocalDate.of(2022, 9, 4)
)

internal val athena = Cat(
    ownerId = owner1,
    id = UUID.fromString("fb153ca2-e241-41df-9fe2-8858c6f6d1b0"),
    name = "Athena",
    nickName = "Goddess",
    born = LocalDate.of(2022, 9, 4)
)
