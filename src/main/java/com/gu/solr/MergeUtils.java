/*

   Copyright 2011 Guardian News and Media

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

*/

package com.gu.solr;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Fieldable;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.update.AddUpdateCommand;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;


public class MergeUtils {

    public static SolrInputDocument merge(SolrInputDocument solrInputDocument, SolrDocument existing, IndexSchema schema, boolean overwriteMultivalues) {
        SolrInputDocument merged = new SolrInputDocument();

        for (String name : existing.getFieldNames()) {
            merged.addField(name, existing.getFieldValue(name));
        }

        for (SolrInputField field : solrInputDocument) {
            String fieldName = field.getName();
            if (!overwriteMultivalues && schema.getField(fieldName).multiValued()) {
                // leave for additions
            } else {
                merged.removeField(fieldName);
            }
        }

        for (SolrInputField field : solrInputDocument) {
            merged.addField(field.getName(), field.getValue());
        }

        return merged;
    }

    public static SolrInputDocument delete(List<String> deleteFields, SolrDocument existing) {
        SolrInputDocument merged = new SolrInputDocument();

        for (String name : existing.getFieldNames()) {
            merged.addField(name, existing.getFieldValue(name));
        }

        for (String name : deleteFields) {
            merged.removeField(name);
        }

        return merged;
    }

    public static List<String> deleteFields(SolrQueryRequest req) {
        String[] fields = req.getParams().getParams("delete.field");

        List<String> deleteParameters;
        if (fields == null || fields.length == 0) {
            deleteParameters = Collections.emptyList();
        } else {
            deleteParameters = Arrays.asList(fields);
        }

        // Process for comma separated
        List<String> deleteFields = new LinkedList<String>();
        for (String parameter : deleteParameters) {
            deleteFields.addAll(Arrays.asList(parameter.split(",")));
        }

        return deleteFields;
    }

    public static SolrDocument toSolrDocument(Document luceneDoc, IndexSchema schema) {
        SolrDocument doc = new SolrDocument();
        addFields(luceneDoc, doc, schema);

        return doc;
    }

    private static void addFields(Document luceneDoc, SolrDocument solrDoc, IndexSchema schema) {
        for (Fieldable f : (List<Fieldable>) luceneDoc.getFields()) {
            SchemaField sf = schema.getField(f.name());
            if (!schema.isCopyFieldTarget(sf)) {
                Object externalVal = sf.getType().toObject(f);
                solrDoc.addField(f.name(), externalVal);
            }
        }
    }

    public static AddUpdateCommand addCommandFor(SolrInputDocument solrDoc) {
        AddUpdateCommand command = new AddUpdateCommand();
        command.solrDoc = solrDoc;
        command.overwriteCommitted = true;

        return command;
    }
}
