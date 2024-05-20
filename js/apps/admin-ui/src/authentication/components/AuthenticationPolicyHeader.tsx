import {useTranslation} from "react-i18next";
import {
    DataListCell,
    DataListDragButton,
    DataListItem,
    DataListItemCells,
    DataListItemRow,
} from "@patternfly/react-core";

import "./authn-policy-header.css";

type AuthenticationPolicyHeaderProps = {
    isParentPolicy?: boolean
};

export const AuthenticationPolicyHeader = ({isParentPolicy}: AuthenticationPolicyHeaderProps) => {
    const {t} = useTranslation();
    return (
        <DataListItem aria-labelledby="headerName" id="header">
            <DataListItemRow>
                <DataListDragButton
                    className="keycloak__authn__policy__header-drag-button"
                    aria-label={t("disabled")}
                />
                <DataListItemCells
                    className="keycloak__authn__policy__header margin-left"
                    dataListCells={[
                        <DataListCell key="step" id="headerName">
                            {t("steps")}
                        </DataListCell>,
                        <DataListCell className={isParentPolicy ? "margin-left" : ""} key="enabled">
                            {t("enabled")}
                        </DataListCell>,
                        <DataListCell key="config"></DataListCell>,
                    ]}
                />
            </DataListItemRow>
        </DataListItem>
    );
};
