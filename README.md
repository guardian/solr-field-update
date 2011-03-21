Solr Field Updates
==================
Basic support for merging documents in Solr.

It is not possible to update individual fields of a Solr indexed document. This can make it impossible to create
a denormalised document if all fields of the document are not simultaneously available at index time.

This project includes `MergeUpdateRequestProcessor`, a Solr update request processor, which supports updating and
deleting individual fields of a document in certain cases. It merges by finding an existing document in the index
and copying unupdated fields over to the new update.

See also: https://issues.apache.org/jira/browse/SOLR-139



Limitations
===========
This library was written with specific indexer requirements. In particular the following limitations were accepted:

   * Because it is necessary to copy fields from the index, all fields must be marked `stored="true"` or they will
     be lost on merge.
   * Documents are only available for this search from the point of the last commit. This means that multiple merges
     without an intermediate commit will only include the last merge. This project was designed for the case where the
     only case for this was a reindex. Because the reindex portion merged all necessary fields, any earlier delete
     could be safely discarded. It would have been necessary to manage intermediate merges and updates in this
     case.
   * Slow.



Usage
=====

Add `MergeUpdateRequestProcessorFactory` to the `updateRequestProcessorChain` in `solrconfig.xml`.

    <updateRequestProcessorChain>
        <processor class="com.gu.solr.MergeUpdateRequestProcessorFactory"/>
        <processor class="solr.RunUpdateProcessorFactory"/>
        <processor class="solr.LogUpdateProcessorFactory"/>
    </updateRequestProcessorChain>

Include the jar for `MergeUpdateRequestProcessorFactory` on your Solr classpath. This may be most simply done by
including it in the `lib` directory of your Solr instance. For more involved use, the jar is available from the
Guardian github repository:

    <dependency>
        <groupId>com.gu</groupId>
        <artifactId>solr-field-update</artifactId>
        <version>0.1</version>
    </dependency>

And check for the latest version number.



Updating
========

To specify a merge update on a solr update add the parameter `merge=true`. By default this appends to multivalued
fields. If you want to overwrite multivalue fields add the parameter `overwriteMultivalues=true`.

To delete a field add `delete.field=<field-name>` to a delete by query or by id. This will remove the specified field,
not remove the record as for standard deletes. Add multiple `delete.field` parameters to remove multiple fields in a
single request.

Comma separated lists are also accepted for `delete.field` values.

A merge by query is also available. Add `merge=true` and `merge.query=<query>` to the request and include a document
as a standard add. This document is merged with all documents that match the given query.
