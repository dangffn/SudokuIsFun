package com.danbuntu.sudokuisfun.ui;

import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

import com.danbuntu.sudokuisfun.R;

import java.util.ArrayList;

/**
 * Created by dan on 7/13/16. Have a great day!
 */
public class StatisticsActivity extends AppCompatActivity {

    ListView lstStatistics;
    StatisticsAdapter adapter;
    PuzzleManager pm;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        lstStatistics = (ListView) findViewById(R.id.lstStatistics);
        adapter = new StatisticsAdapter(this, R.layout.listitem_stats);
        lstStatistics.setAdapter(adapter);

        pm = PuzzleManager.getInstance(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        pm.close();
    }

    private void loadStats() {

        ArrayList<Statistic> stats = new ArrayList<>();

        stats.add(new Statistic("Time Spent Playing Sudoku", pm.getTotalPlayTime(), null));
        stats.add(new Statistic("Longest Play Session", pm.getLongestSession(), null));
        stats.add(new Statistic("New Games Started", pm.getTotalPlayCount(), null));
        stats.add(new Statistic("Puzzles Finished", pm.getTotalFinishCount(), null));
        stats.add(new Statistic("Times Computer Assisted", pm.getTotalAssistCount(), null));

        stats.add(new Statistic("Best Puzzle Finish Time WITHOUT Computer Help", pm.getBestFinishTime(false), pm.getBestFinishPuzzleName(false)));

        stats.add(new Statistic("Best Puzzle Finish Time WITH Computer Help", pm.getBestFinishTime(true), pm.getBestFinishPuzzleName(true)));

        stats.add(new Statistic("Number of puzzles added", pm.getNumberOfPuzzlesAdded(false), null));
        stats.add(new Statistic("Number of puzzles added from an image", pm.getNumberOfPuzzlesAdded(true), null));
        stats.add(new Statistic("Number of puzzles deleted", pm.getNumberOfPuzzlesDeleted(), null));

        stats.add(new Statistic("Most Played Puzzle", pm.getMostPlayedPuzzle(), pm.getLongestPuzzleDuration()));

        adapter.addAll(stats);
        adapter.notifyDataSetChanged();
        pm.close();

    }

    private void clearStats() {
        adapter.clear();
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onStart() {
        super.onStart();
        clearStats();
        loadStats();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch(item.getItemId()) {
            case(R.id.menu_delete_statistics):

                new AlertDialog.Builder(this).setTitle(getString(R.string.message_areYouSure))
                        .setMessage("Are you sure you want to delete all statistics?")
                        .setPositiveButton(getString(R.string.button_yes), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                pm.wipeStatistics();
                                clearStats();
                                loadStats();
                                dialogInterface.dismiss();
                            }
                        })
                        .setNegativeButton(getString(R.string.button_no), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        })
                        .create().show();

                break;
        }

        return super.onOptionsItemSelected(item);
    }
}
