import { useState } from "react";
import { useTranslation } from "react-i18next";
import {
    Button,
    Flex,
    FlexItem,
    Title,
    TitleSizes,
} from "@patternfly/react-core";

import type AuthenticationFlowRepresentation from "@keycloak/keycloak-admin-client/lib/defs/authenticationFlowRepresentation";
import type { AuthenticationProviderRepresentation } from "@keycloak/keycloak-admin-client/lib/defs/authenticatorConfigRepresentation";
import { ListEmptyState } from "../components/list-empty-state/ListEmptyState";
import { AddStepModal } from "./components/modals/AddStepModal";
import { AddSubFlowModal, Flow } from "./components/modals/AddSubFlowModal";

import "./empty-execution-state.css";
import {AddSubPolicyModal, Policy} from "./components/modals/AddSubPolicyModal";

const SECTIONS = ["addCondition", "addSubPolicy"] as const;
type SectionType = (typeof SECTIONS)[number] | undefined;

type EmptyAuthenticationPolicyProps = {
    policy: AuthenticationFlowRepresentation;
    onAddCondition: (type: AuthenticationProviderRepresentation) => void;
    onAddSubPolicy: (flow: Policy) => void;
};

export const EmptyAuthenticationPolicy = ({
                                        policy,
                                        onAddCondition,
                                        onAddSubPolicy,
                                    }: EmptyAuthenticationPolicyProps) => {
    const { t } = useTranslation();
    const [show, setShow] = useState<SectionType>();

    return (
        <>
            {show === "addCondition" && (
                <AddStepModal
                    name={policy.alias!}
                    type={"condition"}
                    onSelect={(type) => {
                        if (type) {
                            onAddCondition(type);
                        }
                        setShow(undefined);
                    }}
                />
            )}
            {show === "addSubPolicy" && (
                <AddSubPolicyModal
                    name={policy.alias!}
                    onCancel={() => setShow(undefined)}
                    onConfirm={(newFlow) => {
                        onAddSubPolicy(newFlow);
                        setShow(undefined);
                    }}
                />
            )}
            <ListEmptyState
                message={t("emptyAuthenticationPolicy")}
                instructions={t("emptyAuthenticationPolicyInstructions")}
            />

            <div className="keycloak__empty-execution-state__block">
                {SECTIONS.map((section) => (
                    <Flex key={section} className="keycloak__empty-execution-state__help">
                        <FlexItem flex={{ default: "flex_1" }}>
                            <Title headingLevel="h2" size={TitleSizes.md}>
                                {t(`${section}Title`)}
                            </Title>
                            <p>{t(section)}</p>
                        </FlexItem>
                        <Flex alignSelf={{ default: "alignSelfCenter" }}>
                            <FlexItem>
                                <Button
                                    data-testid={section}
                                    variant="tertiary"
                                    onClick={() => setShow(section)}
                                >
                                    {t(section)}
                                </Button>
                            </FlexItem>
                        </Flex>
                    </Flex>
                ))}
            </div>
        </>
    );
};
