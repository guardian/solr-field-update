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
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;

import java.io.IOException;


public class Searcher {
    private final IndexSchema schema;
    private final SolrIndexSearcher searcher;
    private final Logger log;

    public Searcher(IndexSchema schema, SolrIndexSearcher searcher, Logger log) {
        this.schema = schema;
        this.searcher = searcher;
        this.log = log;
    }

    public SolrDocument findById(String id) throws IOException {
        if (log.isDebugEnabled()) {
            log.debug(String.format("com.gu.solr.Searcher: Searching for existing document(%s)", id));
        }
        TermQuery query = new TermQuery(new Term(schema.getUniqueKeyField().getName(), id));
        TopDocs results = searcher.search(query, 1);

        SolrDocument doc = null;
        if (results.scoreDocs.length == 1) {
            Document luceneDoc = searcher.doc(results.scoreDocs[0].doc);
            doc = MergeUtils.toSolrDocument(luceneDoc, schema);
        }

        return doc;
    }
}