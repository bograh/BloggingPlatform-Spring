package org.amalitech.bloggingplatformspring.repository;

public interface PostCommentCountProjection {
  Long getPostId();

  Long getTotalComments();
}