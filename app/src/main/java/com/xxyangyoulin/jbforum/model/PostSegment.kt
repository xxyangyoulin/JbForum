package com.xxyangyoulin.jbforum.model

import com.xxyangyoulin.jbforum.PostContentBlock
import com.xxyangyoulin.jbforum.PostItem

sealed class PostSegment {
    abstract val postId: String
    abstract val segmentIndex: Int

    data class First(
        override val postId: String,
        override val segmentIndex: Int,
        val post: PostItem,
        val blocks: List<PostContentBlock>
    ) : PostSegment()

    data class Middle(
        override val postId: String,
        override val segmentIndex: Int,
        val post: PostItem,
        val blocks: List<PostContentBlock>
    ) : PostSegment()

    data class Tail(
        override val postId: String,
        override val segmentIndex: Int,
        val post: PostItem,
        val blocks: List<PostContentBlock>
    ) : PostSegment()

    data class Whole(
        override val postId: String,
        override val segmentIndex: Int,
        val post: PostItem
    ) : PostSegment()
}

fun splitPostSegments(posts: List<PostItem>): List<PostSegment> {
    val result = mutableListOf<PostSegment>()
    for (post in posts) {
        val blocks = post.contentBlocks
        val imageIndices = blocks.mapIndexedNotNull { index, block ->
            if (block.imageUrl != null) index else null
        }
        if (imageIndices.isEmpty()) {
            result.add(PostSegment.Whole(post.pid, 0, post))
            continue
        }
        val firstImgIdx = imageIndices.first()
        val lastImgIdx = imageIndices.last()
        // First segment: blocks[0..firstImgIdx]
        result.add(PostSegment.First(post.pid, 0, post, blocks.subList(0, firstImgIdx + 1)))
        // Middle segments: between adjacent images
        for (i in 0 until imageIndices.size - 1) {
            val from = imageIndices[i] + 1
            val to = imageIndices[i + 1]
            if (from <= to) {
                result.add(PostSegment.Middle(post.pid, i + 1, post, blocks.subList(from, to + 1)))
            }
        }
        // Tail segment: blocks after last image (always present for footer)
        val tailBlocks = if (lastImgIdx + 1 < blocks.size) blocks.subList(lastImgIdx + 1, blocks.size) else emptyList()
        result.add(PostSegment.Tail(post.pid, imageIndices.size, post, tailBlocks))
    }
    return result
}
