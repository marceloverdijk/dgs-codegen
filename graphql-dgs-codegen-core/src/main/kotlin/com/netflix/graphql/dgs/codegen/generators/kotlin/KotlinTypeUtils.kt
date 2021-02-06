/*
 *
 *  Copyright 2020 Netflix, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.netflix.graphql.dgs.codegen.generators.kotlin

import com.netflix.graphql.dgs.codegen.CodeGenConfig
import com.squareup.kotlinpoet.BOOLEAN
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.DOUBLE
import com.squareup.kotlinpoet.INT
import com.squareup.kotlinpoet.LIST
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName as KtTypeName
import com.squareup.kotlinpoet.STRING
import com.squareup.kotlinpoet.typeNameOf
import graphql.language.ListType
import graphql.language.Node
import graphql.language.NodeTraverser
import graphql.language.NodeVisitorStub
import graphql.language.NonNullType
import graphql.language.Type
import graphql.language.TypeName
import graphql.relay.PageInfo
import graphql.util.TraversalControl
import graphql.util.TraverserContext
import java.time.*
import java.util.*

class KotlinTypeUtils(private val packageName: String, val config: CodeGenConfig) {
    @ExperimentalStdlibApi
    private val commonScalars =  mutableMapOf<String, com.squareup.kotlinpoet.TypeName>(
        "LocalTime" to typeNameOf<LocalTime>(),
        "LocalDate" to typeNameOf<LocalDate>(),
        "LocalDateTime" to typeNameOf<LocalDateTime>(),
        "TimeZone" to STRING,
        "Currency" to typeNameOf<Currency>(),
        "Instant" to typeNameOf<Instant>(),
        "DateTime" to typeNameOf<OffsetDateTime>(),
        "RelayPageInfo" to typeNameOf<PageInfo>(),
        "PageInfo" to typeNameOf<PageInfo>(),
        "PresignedUrlResponse" to ClassName.bestGuess("com.netflix.graphql.types.core.resolvers.PresignedUrlResponse"),
        "Header" to ClassName.bestGuess("com.netflix.graphql.types.core.resolvers.PresignedUrlResponse.Header"))


    @ExperimentalStdlibApi
    fun findReturnType(fieldType: Type<*>): KtTypeName {
        val visitor = object : NodeVisitorStub() {
            override fun visitTypeName(node: TypeName, context: TraverserContext<Node<Node<*>>>): TraversalControl {
                context.setAccumulate(node.toKtTypeName().copy(nullable = true))
                return TraversalControl.CONTINUE
            }
            override fun visitListType(node: ListType, context: TraverserContext<Node<Node<*>>>): TraversalControl {
                val typeName = context.getCurrentAccumulate<KtTypeName>()
                context.setAccumulate(LIST.parameterizedBy(typeName).copy(nullable = true))
                return TraversalControl.CONTINUE
            }
            override fun visitNonNullType(node: NonNullType, context: TraverserContext<Node<Node<*>>>): TraversalControl {
                val typeName = context.getCurrentAccumulate<KtTypeName>()
                context.setAccumulate(typeName.copy(nullable = false))
                return TraversalControl.CONTINUE
            }
            override fun visitNode(node: Node<*>, context: TraverserContext<Node<Node<*>>>): TraversalControl {
                throw AssertionError("Unknown field type: $node")
            }
        }
        return NodeTraverser().postOrder(visitor, fieldType) as KtTypeName
    }

    fun isNullable(fieldType: Type<*>): Boolean {
        return fieldType !is NonNullType
    }

    @ExperimentalStdlibApi
    private fun TypeName.toKtTypeName(): KtTypeName {
        if (name in config.typeMapping) {
            return ClassName.bestGuess(config.typeMapping.getValue(name))
        }

        if(commonScalars.containsKey(name)) {
            return commonScalars[name]!!
        }

        return when (name) {
            "String" -> STRING
            "StringValue" -> STRING
            "Int" -> INT
            "IntValue" -> INT
            "Float" -> DOUBLE
            "FloatValue" -> DOUBLE
            "Boolean" -> BOOLEAN
            "BooleanValue" -> BOOLEAN
            "ID" -> STRING
            "IDValue" -> STRING
            else -> ClassName.bestGuess("${packageName}.${name}")
        }
    }

    @ExperimentalStdlibApi
    fun isStringInput(name: com.squareup.kotlinpoet.TypeName): Boolean {
        if (config.typeMapping.containsValue(name.toString())) return when(name.copy(false)) {
            INT -> false
            DOUBLE -> false
            BOOLEAN -> false
            else -> true
        }
        return  name.copy(false) == STRING || commonScalars.containsValue(name.copy(false))
    }
}