/**
 * Bobo Browse Engine - High performance faceted/parametric search implementation 
 * that handles various types of semi-structured data.  Written in Java.
 * 
 * Copyright (C) 2005-2006  John Wang
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 * 
 * To contact the project administrators for the bobo-browse project, 
 * please go to https://sourceforge.net/projects/bobo-browse/, or 
 * send mail to owner@browseengine.com.
 */

package com.browseengine.bobo.api;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.FilterIndexReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.TermEnum;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDocComparator;
import org.apache.lucene.search.SortComparatorSource;
import org.apache.lucene.search.SortField;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.ReaderUtil;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import com.browseengine.bobo.config.impl.XMLFieldConfigurationBuilder;
import com.browseengine.bobo.facets.FacetHandler;
import com.browseengine.bobo.search.LuceneSortDocComparatorFactory;
import com.browseengine.bobo.search.SortFieldEntry;

/**
 * bobo browse index reader
 * 
 */
public class BoboIndexReader extends FilterIndexReader
{
  private static final String                     FIELD_CONFIG  = "field.xml";
  private static final String                     SPRING_CONFIG = "bobo.spring";
  private static Logger                           logger        = Logger.getLogger(BoboIndexReader.class);

  protected Map<String, FacetHandler>               _facetHandlerMap;

  protected Map<SortFieldEntry, ScoreDocComparator> _defaultSortFieldCache;
  protected Collection<FacetHandler>                _facetHandlers;
  protected WorkArea                                _workArea;

  protected IndexReader _srcReader;
  protected BoboIndexReader[] _subReaders = null;
  protected int[] _starts = null;
  
  /**
   * Constructor
   * 
   * @param reader
   *          Index reader
   * @throws IOException
   */
  public static BoboIndexReader getInstance(IndexReader reader) throws IOException
  {
    return BoboIndexReader.getInstance(reader, null, new WorkArea());
  }

  public static BoboIndexReader getInstance(IndexReader reader, WorkArea workArea) throws IOException
  {
    return BoboIndexReader.getInstance(reader, null, workArea);
  }

  /**
   * Constructor.
   * 
   * @param reader
   *          index reader
   * @param facetHandlers
   *          List of facet handlers
   * @throws IOException
   */
  public static BoboIndexReader getInstance(IndexReader reader,
                                            Collection<FacetHandler> facetHandlers) throws IOException
  {
    return BoboIndexReader.getInstance(reader, facetHandlers, new WorkArea());
  }

  public static BoboIndexReader getInstance(IndexReader reader,
                                            Collection<FacetHandler> facetHandlers,
                                            WorkArea workArea) throws IOException
  {
    BoboIndexReader boboReader = new BoboIndexReader(reader, facetHandlers, workArea);
    boboReader.facetInit();
    return boboReader;
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader) throws IOException
  {
    return getInstanceAsSubReader(reader, null, new WorkArea());
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader,
                                                       Collection<FacetHandler> facetHandlers) throws IOException
  {
    return getInstanceAsSubReader(reader, facetHandlers, new WorkArea());
  }

  public static BoboIndexReader getInstanceAsSubReader(IndexReader reader,
                                                       Collection<FacetHandler> facetHandlers,
                                                       WorkArea workArea) throws IOException
  {
    BoboIndexReader boboReader = new BoboIndexReader(reader, facetHandlers, workArea, false);
    boboReader.facetInit();
    return boboReader;
  }
  private static Collection<FacetHandler> loadFromIndex(File file) throws IOException
  {
    File springFile = new File(file, SPRING_CONFIG);
    FileSystemXmlApplicationContext appCtx =
        new FileSystemXmlApplicationContext("file:" + springFile.getAbsolutePath());
    return (Collection<FacetHandler>) appCtx.getBean("handlers");
  }

  @Override
  protected void doClose() throws IOException
  {
    if(_srcReader != null) _srcReader.close();
    super.doClose();
  }
  
  @Override
  protected void doCommit(Map commitUserData) throws IOException
  {
    if(_srcReader != null) _srcReader.flush(commitUserData);
  }

  @Override
  protected void doDelete(int n) throws  CorruptIndexException, IOException
  {
    if(_srcReader != null) _srcReader.deleteDocument(n);
  }
  
  private void loadFacetHandler(String name,
                                Set<String> loaded,
                                Set<String> visited,
                                WorkArea workArea) throws IOException
  {
    FacetHandler facetHandler = _facetHandlerMap.get(name);
    if (facetHandler != null && !loaded.contains(name))
    {
      visited.add(name);
      Set<String> dependsOn = facetHandler.getDependsOn();
      if (dependsOn.size() > 0)
      {
        Iterator<String> iter = dependsOn.iterator();
        while (iter.hasNext())
        {
          String f = iter.next();
          if (name.equals(f))
            continue;
          if (!loaded.contains(f))
          {
            if (visited.contains(f))
            {
              throw new IOException("Facet handler dependency cycle detected, facet handler: "
                  + name + " not loaded");
            }
            loadFacetHandler(f, loaded, visited, workArea);
          }
          if (!loaded.contains(f))
          {
            throw new IOException("unable to load facet handler: " + f);
          }
          facetHandler.putDependedFacetHandler(_facetHandlerMap.get(f));
        }
      }

      long start = System.currentTimeMillis();
      facetHandler.load(this, workArea);
      long end = System.currentTimeMillis();
      if (logger.isDebugEnabled()){
    	StringBuffer buf = new StringBuffer();
    	buf.append("facetHandler loaded: ").append(name).append(", took: ").append(end-start).append(" ms");
        logger.debug(buf.toString());
      }
      loaded.add(name);
    }
  }
  
  private void loadFacetHandlers(WorkArea workArea, Set<String> toBeRemoved)
  {
    Set<String> loaded = new HashSet<String>();
    Set<String> visited = new HashSet<String>();

    for(String name : _facetHandlerMap.keySet())
    {
      try
      {
        loadFacetHandler(name, loaded, visited, workArea);
      }
      catch (Exception ioe)
      {
        toBeRemoved.add(name);
        logger.error("facet load failed: " + name + ": " + ioe.getMessage(), ioe);
      }
    }

    for(String name : toBeRemoved)
    {
      _facetHandlerMap.remove(name);
    }
  }

  private static IndexReader[] createSubReaders(IndexReader reader, WorkArea workArea) throws IOException
  {
    List<IndexReader> readerList = new ArrayList<IndexReader>();
    ReaderUtil.gatherSubReaders(readerList, reader);
    IndexReader[] subReaders = (IndexReader[])readerList.toArray(new IndexReader[readerList.size()]);
    BoboIndexReader[] boboReaders;
    
    if(subReaders != null && subReaders.length > 0)
    {
      boboReaders = new BoboIndexReader[subReaders.length];
      for(int i = 0; i < subReaders.length; i++)
      {
        boboReaders[i] = new BoboIndexReader(subReaders[i], null, workArea, false);
      }
    }
    else
    {
      boboReaders = new BoboIndexReader[]{ new BoboIndexReader(reader, null, workArea, false) };
    }
    return boboReaders;
  }
  
  @Override
  public Directory directory()
  {
    return (_subReaders != null ? _subReaders[0].directory() : super.directory());
  }
  
  protected void initialize(Collection<FacetHandler> facetHandlers) throws IOException
  {
    if (facetHandlers == null) // try to load from index
    {
      Directory idxDir = directory();
      if (idxDir != null && idxDir instanceof FSDirectory)
      {
        FSDirectory fsDir = (FSDirectory) idxDir;
        File file = fsDir.getFile();

        if (new File(file, SPRING_CONFIG).exists())
        {
          facetHandlers = loadFromIndex(file);
        }
        else if (new File(file, FIELD_CONFIG).exists())
        {
          facetHandlers =
              XMLFieldConfigurationBuilder.loadFieldConfiguration(new File(file,
                                                                           FIELD_CONFIG))
                                          .getFacetHandlers();
        }
        else
        {
          facetHandlers = new ArrayList<FacetHandler>();
        }
      }
      else
      {
        facetHandlers = new ArrayList<FacetHandler>();
      }
    }
    
    _facetHandlers = facetHandlers;
    _facetHandlerMap = new HashMap<String, FacetHandler>();
    for (FacetHandler facetHandler : facetHandlers)
    {
      _facetHandlerMap.put(facetHandler.getName(), facetHandler);
    }
    _defaultSortFieldCache = new HashMap<SortFieldEntry, ScoreDocComparator>();
  }

  public ScoreDocComparator getDefaultScoreDocComparator(SortField f) throws IOException
  {
    int type = f.getType();
    if (type == SortField.DOC)
      return ScoreDocComparator.INDEXORDER;
    if (type == SortField.SCORE)
      return ScoreDocComparator.RELEVANCE;

    SortComparatorSource factory = f.getFactory();
    SortFieldEntry entry = factory == null 
                         ? new SortFieldEntry(f.getField(), type, f.getLocale())
                         : new SortFieldEntry(f.getField(), factory);
    ScoreDocComparator comparator = _defaultSortFieldCache.get(entry);
    if (comparator == null)
    {
      synchronized (_defaultSortFieldCache)
      {
        comparator = _defaultSortFieldCache.get(entry);
        if (comparator == null)
        {
          comparator = LuceneSortDocComparatorFactory.buildScoreDocComparator(this, entry);
          if (comparator!=null){
            _defaultSortFieldCache.put(entry, comparator);
          }
        }
      }
      return comparator;
    }
    else
    {
      return comparator;
    }
  }

  protected BoboIndexReader(IndexReader reader,
                            Collection<FacetHandler> facetHandlers,
                            WorkArea workArea) throws IOException
  {
    this(reader, facetHandlers, workArea, true);
    _srcReader = reader;
  }
  
  protected BoboIndexReader(IndexReader reader,
                            Collection<FacetHandler> facetHandlers,
                            WorkArea workArea,
                            boolean useSubReaders) throws IOException
  {
    super(useSubReaders ? new MultiReader(createSubReaders(reader, workArea), false) : reader);
    
    if(useSubReaders)
    {
      BoboIndexReader[] subReaders = (BoboIndexReader[])in.getSequentialSubReaders();
      if(subReaders != null && subReaders.length > 0)
      {
        _subReaders = subReaders;
        
        int maxDoc = 0;
        _starts = new int[_subReaders.length + 1];
        for (int i = 0; i < _subReaders.length; i++)
        {
          if(facetHandlers != null) _subReaders[i].setFacetHandlers(cloneFacetHandlers(facetHandlers));
          _starts[i] = maxDoc;
          maxDoc += _subReaders[i].maxDoc();
        }
        _starts[_subReaders.length] = maxDoc;
      }
    }
    _facetHandlers = facetHandlers;
    _workArea = workArea;
  }
  
  private static Collection<FacetHandler> cloneFacetHandlers(Collection<FacetHandler> facetHandlers)
  {
    ArrayList<FacetHandler> clonedFacetHandlers = new ArrayList<FacetHandler>(facetHandlers.size());
    for(FacetHandler f : facetHandlers)
    {
      try
      {
        clonedFacetHandlers.add((FacetHandler)f.clone());
      }
      catch (CloneNotSupportedException e)
      {
        throw new RuntimeException(e.getMessage(),e);
      }
    }
    return clonedFacetHandlers;
  }

  protected void facetInit() throws IOException
  {
    facetInit(new HashSet<String>());
  }
  
  protected void facetInit(Set<String> toBeRemoved) throws IOException
  {
    initialize(_facetHandlers);
    if(_subReaders == null)
    {
      loadFacetHandlers(_workArea, toBeRemoved);  
    }
    else
    {
      for(BoboIndexReader r : _subReaders)
      {
        r.facetInit(toBeRemoved);
      }
      
      for(String name : toBeRemoved)
      {
        _facetHandlerMap.remove(name);
      }
    }
  }

  protected void setFacetHandlers(Collection<FacetHandler> facetHandlers)
  {
    _facetHandlers = facetHandlers;
  }
  /**
   * @deprecated use {@link org.apache.lucene.search.MatchAllDocsQuery} instead.
   * @return
   */
  public Query getFastMatchAllDocsQuery()
  {
    return new MatchAllDocsQuery();
  }

  /**
   * Utility method to dump out all fields (name and terms) for a given index.
   * 
   * @param outFile
   *          File to dump to.
   * @throws IOException
   */
  public void dumpFields(File outFile) throws IOException
  {
    FileWriter writer = null;
    try
    {
      writer = new FileWriter(outFile);
      PrintWriter out = new PrintWriter(writer);
      Set<String> fieldNames = getFacetNames();
      for (String fieldName : fieldNames)
      {
        TermEnum te = terms(new Term(fieldName, ""));
        out.write(fieldName + ":\n");
        while (te.next())
        {
          Term term = te.term();
          if (!fieldName.equals(term.field()))
          {
            break;
          }
          out.write(term.text() + "\n");
        }
        out.write("\n\n");
      }
    }
    finally
    {
      if (writer != null)
      {
        writer.close();
      }
    }
  }

  /**
   * Gets all the facet field names
   * 
   * @return Set of facet field names
   */
  public Set<String> getFacetNames()
  {
    return _facetHandlerMap.keySet();
  }

  /**
   * Gets a facet handler
   * 
   * @param fieldname
   *          name
   * @return facet handler
   */
  public FacetHandler getFacetHandler(String fieldname)
  {
    return _facetHandlerMap.get(fieldname);
  }
  
  

  @Override
  public IndexReader[] getSequentialSubReaders() {
	return _subReaders;
  }

/**
   * Gets the facet handler map
   * 
   * @return facet handler map
   */
  public Map<String, FacetHandler> getFacetHandlerMap()
  {
    return _facetHandlerMap;
  }
  
  public void rewrap(IndexReader in){
    if(_subReaders != null)
    {
      throw new UnsupportedOperationException("this BoboIndexReader has subreaders");
    }
    super.in = in;
  }

  @Override
  public Document document(int docid) throws IOException
  {
    if(_subReaders != null)
    {
      int readerIndex = readerIndex(docid, _starts, _subReaders.length);
      BoboIndexReader subReader = _subReaders[readerIndex];
      return subReader.document(docid - _starts[readerIndex]);
    }
    else
    {
      Document doc = super.document(docid);
      Collection<FacetHandler> facetHandlers = _facetHandlerMap.values();
      for (FacetHandler facetHandler : facetHandlers)
      {
        String[] vals = facetHandler.getFieldValues(docid);
        if (vals != null)
        {
          for (String val : vals)
          {
            doc.add(new Field(facetHandler.getName(),
                              val,
                              Field.Store.NO,
                              Field.Index.NOT_ANALYZED));
          }
        }
      }
      return doc;
    }
  }
  
  private static int readerIndex(int n, int[] starts, int numSubReaders)
  {
    int lo = 0;
    int hi = numSubReaders - 1;

    while (hi >= lo)
    {
      int mid = (lo + hi) >>> 1;
      int midValue = starts[mid];
      if (n < midValue)
        hi = mid - 1;
      else if (n > midValue)
        lo = mid + 1;
      else
      {
        while (mid+1 < numSubReaders && starts[mid+1] == midValue)
        {
          mid++;
        }
        return mid;
      }
    }
    return hi;
  }

  /**
   * Work area for loading
   */
  public static class WorkArea
  {
    private HashMap<Class<?>, Object> map = new HashMap<Class<?>, Object>();

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> cls)
    {
      T obj = (T) map.get(cls);
      return obj;
    }

    public void put(Object obj)
    {
      map.put(obj.getClass(), obj);
    }

    public void clear()
    {
      map.clear();
    }
  }
}
