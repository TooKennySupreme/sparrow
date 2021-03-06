package com.sparrowwallet.sparrow.control;

import com.sparrowwallet.drongo.BitcoinUnit;
import com.sparrowwallet.drongo.wallet.Wallet;
import com.sparrowwallet.sparrow.io.Config;
import com.sparrowwallet.sparrow.wallet.TransactionEntry;
import com.sparrowwallet.sparrow.wallet.WalletTransactionsEntry;
import javafx.beans.NamedArg;
import javafx.scene.Node;
import javafx.scene.chart.*;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class BalanceChart extends LineChart<Number, Number> {
    private XYChart.Series<Number, Number> balanceSeries;

    private TransactionEntry selectedEntry;

    public BalanceChart(@NamedArg("xAxis") Axis<Number> xAxis, @NamedArg("yAxis") Axis<Number> yAxis) {
        super(xAxis, yAxis);
    }

    public void initialize(WalletTransactionsEntry walletTransactionsEntry) {
        managedProperty().bind(visibleProperty());
        balanceSeries = new XYChart.Series<>();
        getData().add(balanceSeries);
        update(walletTransactionsEntry);

        BitcoinUnit unit = Config.get().getBitcoinUnit();
        setBitcoinUnit(walletTransactionsEntry.getWallet(), unit);
    }

    public void update(WalletTransactionsEntry walletTransactionsEntry) {
        setVisible(!walletTransactionsEntry.getChildren().isEmpty());
        balanceSeries.getData().clear();

        List<Data<Number, Number>> balanceDataList = walletTransactionsEntry.getChildren().stream()
                .map(entry -> (TransactionEntry)entry)
                .filter(txEntry -> txEntry.getBlockTransaction().getHeight() > 0)
                .map(txEntry -> new XYChart.Data<>((Number)txEntry.getBlockTransaction().getDate().getTime(), (Number)txEntry.getBalance(), txEntry))
                .collect(Collectors.toList());

        int size = balanceDataList.size() * 2;
        for(int i = 0; i < size; i+= 2) {
            Data<Number, Number> data = balanceDataList.get(i);

            if(i + 1 < balanceDataList.size()) {
                Data<Number, Number> nextData = balanceDataList.get(i + 1);
                Data<Number, Number> interstitialData = new Data<>(nextData.getXValue(), data.getYValue(), null);
                balanceDataList.add(i + 1, interstitialData);
            } else {
                Date now = new Date();
                Data<Number, Number> interstitialData = new Data<>(now.getTime(), data.getYValue(), null);
                balanceDataList.add(interstitialData);
            }
        }

        if(!balanceDataList.isEmpty()) {
            long min = balanceDataList.stream().map(data -> data.getXValue().longValue()).min(Long::compare).get();
            long max = balanceDataList.stream().map(data -> data.getXValue().longValue()).max(Long::compare).get();

            DateAxisFormatter dateAxisFormatter = new DateAxisFormatter(max - min);
            NumberAxis xAxis = (NumberAxis)getXAxis();
            xAxis.setTickLabelFormatter(dateAxisFormatter);
        }

        balanceSeries.getData().addAll(balanceDataList);

        if(selectedEntry != null) {
            select(selectedEntry);
        }
    }

    public void select(TransactionEntry transactionEntry) {
        Set<Node> selectedSymbols = lookupAll(".chart-line-symbol.selected");
        for(Node selectedSymbol : selectedSymbols) {
            selectedSymbol.getStyleClass().remove("selected");
        }

        for(int i = 0; i < balanceSeries.getData().size(); i++) {
            XYChart.Data<Number, Number> data = balanceSeries.getData().get(i);
            Node symbol = lookup(".chart-line-symbol.data" + i);
            if(symbol != null) {
                if(transactionEntry.getBlockTransaction().getDate() != null && data.getXValue().equals(transactionEntry.getBlockTransaction().getDate().getTime()) && data.getExtraValue() != null) {
                    symbol.getStyleClass().add("selected");
                    selectedEntry = transactionEntry;
                }
            }
        }
    }

    public void setBitcoinUnit(Wallet wallet, BitcoinUnit unit) {
        if(unit == null || unit.equals(BitcoinUnit.AUTO)) {
            unit = wallet.getAutoUnit();
        }

        NumberAxis yaxis = (NumberAxis)getYAxis();
        yaxis.setTickLabelFormatter(new CoinAxisFormatter(yaxis, unit));
    }
}
