package com.example.rv_page

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.chat.rv_page.CarouselSnap
import com.chat.rv_page.RvPage
import com.example.rv_page.databinding.ActivityMainBinding
import com.example.rv_page.databinding.ItemFeatureCardBinding
import com.example.rv_page.databinding.ItemFeedRowBinding
import com.example.rv_page.databinding.ItemSectionTitleBinding

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        setupRvPageDemo()
    }

    private fun setupRvPageDemo() {
        val featureCards = listOf(
            FeatureCard(1, "Static sections", "Header, carousel and list share one RecyclerView."),
            FeatureCard(2, "Diff updates", "Use keyOf to generate a simple DiffUtil callback."),
            FeatureCard(3, "Click events", "Section builders expose item click callbacks.")
        )

        val feedItems = listOf(
            FeedItem(1, "Build page level RecyclerView", "Use RvPage.with(owner) to assemble sections."),
            FeedItem(2, "Add horizontal carousel", "carousel(...) creates a nested horizontal RecyclerView."),
            FeedItem(3, "Update section data", "Keep the returned handle and call submit(newList)."),
            FeedItem(4, "Mix with paging later", "Replace list(...) with pagingList(...) for PagingData.")
        )

        RvPage.with(this)
            .recyclerView(binding.recyclerView)
            .layoutManager(LinearLayoutManager(this))
            .sections {
                single<String, ItemSectionTitleBinding>(
                    ItemSectionTitleBinding::inflate,
                    tag = "page_title"
                ) {
                    data = "RvPage usage demo"
                    onBind { itemBinding, title ->
                        itemBinding.titleText.text = title
                    }
                }

                carousel<FeatureCard, ItemFeatureCardBinding>(
                    ItemFeatureCardBinding::inflate,
                    tag = "feature_carousel"
                ) {
                    keyOf { it.id }
                    data = featureCards
                    heightDp = 132
                    itemSpacingDp = 12
                    paddingBottomDp = 8
                    snap = CarouselSnap.LINEAR
                    onBind { itemBinding, item, _ ->
                        itemBinding.titleText.text = item.title
                        itemBinding.summaryText.text = item.summary
                    }
                    onItemClick(throttleMs = 500, keyOf = { it.id }) { _, item, _ ->
                        Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
                    }
                }

                list<FeedItem, ItemFeedRowBinding>(
                    ItemFeedRowBinding::inflate,
                    tag = "feed_list"
                ) {
                    keyOf { it.id }
                    data = feedItems
                    onBind { itemBinding, item, _ ->
                        itemBinding.titleText.text = item.title
                        itemBinding.summaryText.text = item.summary
                    }
                    onItemClick(throttleMs = 500, keyOf = { it.id }) { _, item, _ ->
                        Toast.makeText(this@MainActivity, item.title, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .start()
    }

    private data class FeatureCard(
        val id: Long,
        val title: String,
        val summary: String
    )

    private data class FeedItem(
        val id: Long,
        val title: String,
        val summary: String
    )
}
