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
import org.apache.lucene.search.Query;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.QueryParsing;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.DeleteUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;


public class MergeUpdateRequestProcessorFactory extends UpdateRequestProcessorFactory {
    @Override
    public UpdateRequestProcessor getInstance(SolrQueryRequest req, SolrQueryResponse rsp, UpdateRequestProcessor next) {
        if (req.getParams().getBool("merge", false)) {
            return new MergeUpdateRequestProcessor(req, next);
        }

        return next;
    }
}

class MergeUpdateRequestProcessor extends UpdateRequestProcessor {
    private final Logger log;
    private final IndexSchema schema;
    private final SolrIndexSearcher searcher;
    private final boolean overwriteMultivalues;
    private final List<String> deleteFields;
    private final String mergeQuery;

    public MergeUpdateRequestProcessor(SolrQueryRequest req, UpdateRequestProcessor next) {
        super(next);
        this.log = SolrCore.log;
        this.schema = req.getSchema();
        this.searcher = req.getSearcher();
        this.overwriteMultivalues = req.getParams().getBool("overwriteMultivalues", true);
        this.deleteFields = MergeUtils.deleteFields(req);
        this.mergeQuery = req.getParams().get("merge.query");
    }

    @Override
    public void processAdd(AddUpdateCommand cmd) throws IOException {
        if (mergeQuery == null || mergeQuery.trim().equals("")) {
            String id = cmd.getIndexedId(schema);
            log.debug("MergeUpdateRequest: add " + id);

            Searcher search = new Searcher(schema, searcher, log);
            SolrDocument doc = search.findById(id);

            if (doc != null) {
                if (log.isDebugEnabled()) {
                    log.debug("MergeUpdateRequest: Merging with existing document.");
                }

                cmd.solrDoc = MergeUtils.merge(cmd.getSolrInputDocument(), doc, schema, overwriteMultivalues);
            } else {
                if (log.isDebugEnabled()) {
                    log.debug("MergeUpdateRequest: New insert.");
                }
            }

            super.processAdd(cmd);

        } else {
            if (log.isDebugEnabled()) {
                log.debug("MergeUpdateRequest: add " + mergeQuery);
                log.debug("MergeUpdateRequest: Merge into existing documents(%s)", mergeQuery);
            }

            Query q = QueryParsing.parseQuery(mergeQuery, schema);
            DocIterator docs = searcher.getDocSet(q).iterator();

            SolrInputDocument merge = MergeUtils.withoutId(cmd.getSolrInputDocument(), schema);

            while (docs.hasNext()) {
                Document luceneDoc = searcher.doc(docs.nextDoc());
                SolrDocument doc = MergeUtils.toSolrDocument(luceneDoc, schema);
                SolrInputDocument merged = MergeUtils.merge(merge, doc, schema, overwriteMultivalues);
                log.debug("MergeUpdateRequest: merged = " + merged);

                super.processAdd(MergeUtils.addCommandFor(merged));
            }
        }
    }

    @Override
    public void processDelete(DeleteUpdateCommand cmd) throws IOException {
        if (cmd.id != null) {
            String id = cmd.id;
            log.debug("MergeUpdateRequest: delete " + id);

            Searcher search = new Searcher(schema, searcher, log);
            SolrDocument doc = search.findById(id);

            if (doc != null) {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("MergeUpdateRequest: Deleting from existing document(%s)", id));
                }

                SolrInputDocument merged = MergeUtils.delete(deleteFields, doc);

                super.processAdd(MergeUtils.addCommandFor(merged));
            } else {
                if (log.isDebugEnabled()) {
                    log.debug(String.format("MergeUpdateRequest: Delete from unknown document(id=%s)", id));
                }
            }
        }

        if (cmd.query != null) {
            String query = cmd.query;

            if (log.isDebugEnabled()) {
                log.debug("MergeUpdateRequest: delete " + query);
                log.debug("MergeUpdateRequest: Delete from existing documents(%s)", query);
            }

            Query q = QueryParsing.parseQuery(query, schema);
            DocIterator docs = searcher.getDocSet(q).iterator();

            while (docs.hasNext()) {
                Document luceneDoc = searcher.doc(docs.nextDoc());
                SolrDocument doc = MergeUtils.toSolrDocument(luceneDoc, schema);
                SolrInputDocument merged = MergeUtils.delete(deleteFields, doc);

                super.processAdd(MergeUtils.addCommandFor(merged));
            }
        }
    }
}