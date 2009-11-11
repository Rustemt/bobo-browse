package com.browseengine.bobo.score;

import com.browseengine.bobo.api.BrowseException;
import com.browseengine.bobo.api.BrowseRequest;

/**
 * creates a Score Adjuster per request.
 * 
 */
public interface ScoreAdjusterFactory {
    ScoreAdjuster newScoreAdjuster(BrowseRequest br) throws BrowseException;
}

