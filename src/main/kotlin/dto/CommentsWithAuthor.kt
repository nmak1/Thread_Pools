package dto

data class CommentsWithAuthor(val comment: Comment,
                              val author: List<Author>
                              )
