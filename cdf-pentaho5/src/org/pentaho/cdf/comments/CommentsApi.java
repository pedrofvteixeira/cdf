/*!
 * Copyright 2002 - 2015 Webdetails, a Pentaho company. All rights reserved.
 *
 * This software was developed by Webdetails and is provided under the terms
 * of the Mozilla Public License, Version 2.0, or any later version. You may not use
 * this file except in compliance with the license. If you need a copy of the license,
 * please go to http://mozilla.org/MPL/2.0/. The Initial Developer is Webdetails.
 *
 * Software distributed under the Mozilla Public License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. Please refer to
 * the license for the specific language governing your rights and limitations.
 */

package org.pentaho.cdf.comments;

import static javax.ws.rs.core.MediaType.APPLICATION_FORM_URLENCODED;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_XML;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONObject;
import org.pentaho.cdf.util.Parameter;
import org.pentaho.cdf.utils.CorsUtil;

import pt.webdetails.cpf.session.IUserSession;
import pt.webdetails.cpf.session.PentahoSession;
import pt.webdetails.cpf.utils.PluginIOUtils;

@Path( "/pentaho-cdf/api/comments" )
public class CommentsApi {

  private static final Log logger = LogFactory.getLog( CommentsApi.class );

  private IUserSession userSession = new PentahoSession();

  @GET
  @Path( "/add" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON, APPLICATION_FORM_URLENCODED } )
  public void add( @DefaultValue( "" ) @QueryParam( Parameter.PAGE ) String page,
      @DefaultValue( "" ) @QueryParam( Parameter.COMMENT ) String comment,
      @Context HttpServletResponse servletResponse, @Context HttpServletRequest servletRequest ) {

    JSONObject json;
    String result = "";

    try {
      json = CommentsEngine.getInstance().add( page, comment, getUserName() );
      result = json.toString( 2 );
    } catch ( Exception e ) {
      logger.error( "Error processing comment: " + e );
    }

    try {
      PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), result );
      CorsUtil.getInstance().setCorsHeaders( servletRequest, servletResponse );
    } catch ( IOException ex ) {
      logger.error( "Error while outputting result", ex );
    }
  }

  @GET
  @Path( "/list" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON, APPLICATION_FORM_URLENCODED } )
  public void list( @DefaultValue( "" ) @QueryParam( Parameter.PAGE ) String page,
      @DefaultValue( "0" ) @QueryParam( Parameter.FIRST_RESULT ) int firstResult,
      @DefaultValue( "20" ) @QueryParam( Parameter.MAX_RESULTS ) int maxResults,
      @DefaultValue( "false" ) @QueryParam( Parameter.DELETED ) boolean deleted,
      @DefaultValue( "false" ) @QueryParam( Parameter.ARCHIVED ) boolean archived,

      @Context HttpServletResponse servletResponse, @Context HttpServletRequest servletRequest ) {

    JSONObject json;
    String result = "";

    boolean isAdministrator = new PentahoSession().isAdministrator();

    if ( deleted && !isAdministrator ) {
      deleted = false;
      logger.warn( "only admin users are allowed to see deleted comments" );
    }

    if ( archived && !isAdministrator ) {
      archived = false;
      logger.warn( "only admin users are allowed to see archived comments" );
    }

    try {
      json = CommentsEngine.getInstance().list( page, firstResult, maxResults, deleted, archived, getUserName() );
      result = json.toString( 2 );
    } catch ( Exception e ) {
      logger.error( "Error processing comment: " + e );
    }

    try {
      PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), result );
      CorsUtil.getInstance().setCorsHeaders( servletRequest, servletResponse );
    } catch ( IOException ex ) {
      logger.error( "Error while outputting result", ex );
    }
  }

  @GET
  @Path( "/archive" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON, APPLICATION_FORM_URLENCODED } )
  public void archive( @DefaultValue( "0" ) @QueryParam( Parameter.COMMENT_ID ) int commentId,
      @DefaultValue( "true" ) @QueryParam( Parameter.VALUE ) boolean value,
      @Context HttpServletResponse servletResponse, @Context HttpServletRequest servletRequest ) {

    JSONObject json;
    String result = "";

    boolean isAdministrator = getUserSession().isAdministrator();
    boolean isAuthenticated = getUserSession().isAuthenticated();

    if ( !isAuthenticated ) {
      String msg = "Operation not authorized: requires authentication";
      logger.error( msg );
      try {
        PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), msg );
      } catch ( IOException ex ) {
        logger.error( "Error while outputting result", ex );
      }
      return;
    }

    try {
      json = CommentsEngine.getInstance().archive( commentId, value, getUserName(), isAdministrator );
      result = json.toString( 2 );
    } catch ( Exception e ) {
      logger.error( "Error processing comment: " + e );
    }

    try {
      PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), result );
      CorsUtil.getInstance().setCorsHeaders( servletRequest, servletResponse );
    } catch ( IOException ex ) {
      logger.error( "Error while outputting result", ex );
    }
  }

  @GET
  @Path( "/delete" )
  @Consumes( { APPLICATION_XML, APPLICATION_JSON, APPLICATION_FORM_URLENCODED } )
  public void delete( @DefaultValue( "0" ) @QueryParam( "commentId" ) int commentId,
      @DefaultValue( "true" ) @QueryParam( Parameter.VALUE ) boolean value,
      @Context HttpServletResponse servletResponse, @Context HttpServletRequest servletRequest ) {

    JSONObject json;
    String result = "";

    boolean isAdministrator = getUserSession().isAdministrator();
    boolean isAuthenticated = getUserSession().isAuthenticated();

    if ( !isAuthenticated ) {
      String msg = "Operation not authorized: requires authentication";
      logger.error( msg );
      try {
        PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), msg );
      } catch ( IOException ex ) {
        logger.error( "Error while outputting result", ex );
      }
      return;
    }

    try {
      json = CommentsEngine.getInstance().delete( commentId, value, getUserSession().getUserName(), isAdministrator );
      result = json.toString( 2 );
    } catch ( Exception ex ) {
      logger.error( "Error processing comment: " + ex );
    }

    try {
      PluginIOUtils.writeOutAndFlush( servletResponse.getOutputStream(), result );
      CorsUtil.getInstance().setCorsHeaders( servletRequest, servletResponse );
    } catch ( IOException ex ) {
      logger.error( "Error while outputting result", ex );
    }
  }

  protected String getUserName(){
    return getUserSession().getUserName();
  }

  protected IUserSession getUserSession(){
    return userSession;
  }
}
