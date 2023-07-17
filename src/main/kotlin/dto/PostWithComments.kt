package dto

data class PostWithComments(val post: Post,
                            val author: List<Author>,
                            val comments: List<CommentsWithAuthor>
                            )
